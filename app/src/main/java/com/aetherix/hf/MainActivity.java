package com.aetherix.hf;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {

    private static final int SEARCH_LIMIT = 25;
    private static final int CONNECT_TIMEOUT = 10000;
    private static final int READ_TIMEOUT = 15000;
    private static final String USER_AGENT = "HFModelDownloader/2.0 (Android; +https://github.com/DroidSpectre/hf-downloader)";
    private static final String PREF_KEY_TOKEN = "hf_token";

    private static final String STATE_SEARCH_QUERY = "search_query";
    private static final String STATE_STATUS_TEXT = "status_text";
    private static final String STATE_IS_LOADING = "is_loading";
    private static final String STATE_MODEL_IDS = "model_ids";
    private static final String STATE_MODEL_DOWNLOADS = "model_downloads";
    private static final String STATE_MODEL_LAST_MODIFIED = "model_last_modified";
    private static final String STATE_MODEL_TAGS = "model_tags";
    private static final String STATE_MODEL_PIPELINE_TAG = "model_pipeline_tag";
    private static final String STATE_MODEL_LIBRARY_NAME = "model_library_name";
    private static final String STATE_MODEL_LIKES = "model_likes";
    private static final String STATE_MODEL_IS_PRIVATE = "model_is_private";
    private static final String STATE_MODEL_IS_GATED = "model_is_gated";

    private EditText searchEdit;
    private Button searchButton;
    private ImageButton settingsButton;
    private ListView modelsListView;
    private ProgressBar loadingProgress;
    private TextView statusText;

    private List<HuggingFaceModel> modelsList;
    private ModelsAdapter modelsAdapter;

    private ExecutorService executor;
    private Handler mainHandler;
    private SharedPreferences prefs;
    private String hfToken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        executor = Executors.newFixedThreadPool(2);
        mainHandler = new Handler(Looper.getMainLooper());
        prefs = getPreferences(MODE_PRIVATE);
        hfToken = prefs.getString(PREF_KEY_TOKEN, "");

        initializeViews();
        setupListeners();

        modelsList = new ArrayList<HuggingFaceModel>();
        modelsAdapter = new ModelsAdapter(this, modelsList);
        modelsListView.setAdapter(modelsAdapter);

        if (savedInstanceState != null) {
            restoreInstanceState(savedInstanceState);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_SEARCH_QUERY, searchEdit.getText().toString());
        outState.putString(STATE_STATUS_TEXT, statusText.getText().toString());
        outState.putBoolean(STATE_IS_LOADING, loadingProgress.getVisibility() == View.VISIBLE);

        ArrayList<String> ids = new ArrayList<String>();
        ArrayList<Integer> downloads = new ArrayList<Integer>();
        ArrayList<String> lastModified = new ArrayList<String>();
        ArrayList<String> tags = new ArrayList<String>();
        ArrayList<String> pipelineTags = new ArrayList<String>();
        ArrayList<String> libraryNames = new ArrayList<String>();
        ArrayList<Integer> likes = new ArrayList<Integer>();
        ArrayList<Boolean> isPrivate = new ArrayList<Boolean>();
        ArrayList<Boolean> isGated = new ArrayList<Boolean>();

        for (HuggingFaceModel model : modelsList) {
            ids.add(model.getId());
            downloads.add(model.getDownloads());
            lastModified.add(model.getLastModified());
            tags.add(model.getTags());
            pipelineTags.add(model.getPipelineTag());
            libraryNames.add(model.getLibraryName());
            likes.add(model.getLikes());
            isPrivate.add(model.isPrivate());
            isGated.add(model.isGated());
        }

        outState.putStringArrayList(STATE_MODEL_IDS, ids);
        outState.putIntegerArrayList(STATE_MODEL_DOWNLOADS, downloads);
        outState.putStringArrayList(STATE_MODEL_LAST_MODIFIED, lastModified);
        outState.putStringArrayList(STATE_MODEL_TAGS, tags);
        outState.putStringArrayList(STATE_MODEL_PIPELINE_TAG, pipelineTags);
        outState.putStringArrayList(STATE_MODEL_LIBRARY_NAME, libraryNames);
        outState.putIntegerArrayList(STATE_MODEL_LIKES, likes);
        boolean[] isPrivateArr = new boolean[isPrivate.size()];
        for (int b = 0; b < isPrivate.size(); b++) isPrivateArr[b] = isPrivate.get(b);
        outState.putBooleanArray(STATE_MODEL_IS_PRIVATE, isPrivateArr);
        boolean[] isGatedArr = new boolean[isGated.size()];
        for (int b = 0; b < isGated.size(); b++) isGatedArr[b] = isGated.get(b);
        outState.putBooleanArray(STATE_MODEL_IS_GATED, isGatedArr);
    }

    private void restoreInstanceState(Bundle savedInstanceState) {
        String query = savedInstanceState.getString(STATE_SEARCH_QUERY, "");
        searchEdit.setText(query);

        String status = savedInstanceState.getString(STATE_STATUS_TEXT, "");
        statusText.setText(status);

        boolean isLoading = savedInstanceState.getBoolean(STATE_IS_LOADING, false);
        setLoadingState(isLoading);

        ArrayList<String> ids = savedInstanceState.getStringArrayList(STATE_MODEL_IDS);
        if (ids != null && !ids.isEmpty()) {
            ArrayList<Integer> downloads = savedInstanceState.getIntegerArrayList(STATE_MODEL_DOWNLOADS);
            ArrayList<String> lastModified = savedInstanceState.getStringArrayList(STATE_MODEL_LAST_MODIFIED);
            ArrayList<String> tagsList = savedInstanceState.getStringArrayList(STATE_MODEL_TAGS);
            ArrayList<String> pipelineTags = savedInstanceState.getStringArrayList(STATE_MODEL_PIPELINE_TAG);
            ArrayList<String> libraryNames = savedInstanceState.getStringArrayList(STATE_MODEL_LIBRARY_NAME);
            ArrayList<Integer> likes = savedInstanceState.getIntegerArrayList(STATE_MODEL_LIKES);
            boolean[] isPrivateArr = savedInstanceState.getBooleanArray(STATE_MODEL_IS_PRIVATE);
            ArrayList<Boolean> isPrivateList = new ArrayList<Boolean>();
            if (isPrivateArr != null) { for (boolean b : isPrivateArr) isPrivateList.add(b); }
            boolean[] isGatedArr = savedInstanceState.getBooleanArray(STATE_MODEL_IS_GATED);
            ArrayList<Boolean> isGatedList = new ArrayList<Boolean>();
            if (isGatedArr != null) { for (boolean b : isGatedArr) isGatedList.add(b); }

            modelsList.clear();
            for (int i = 0; i < ids.size(); i++) {
                modelsList.add(new HuggingFaceModel(
                    ids.get(i),
                    downloads != null ? downloads.get(i) : 0,
                    lastModified != null ? lastModified.get(i) : "",
                    tagsList != null ? tagsList.get(i) : "",
                    pipelineTags != null ? pipelineTags.get(i) : "",
                    libraryNames != null ? libraryNames.get(i) : "",
                    likes != null ? likes.get(i) : 0,
                    isPrivateList != null ? isPrivateList.get(i) : false,
                    isGatedList != null ? isGatedList.get(i) : false
                ));
            }
            modelsAdapter.notifyDataSetChanged();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executor != null && !executor.isShutdown()) {
            executor.shutdownNow();
        }
    }

    private void initializeViews() {
        searchEdit = findViewById(R.id.searchEdit);
        searchButton = findViewById(R.id.searchButton);
        settingsButton = findViewById(R.id.settingsButton);
        modelsListView = findViewById(R.id.modelsListView);
        loadingProgress = findViewById(R.id.loadingProgress);
        statusText = findViewById(R.id.statusText);
    }

    private void setupListeners() {
        if (searchButton != null) {
            searchButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    performSearch();
                }
            });
        }

        if (settingsButton != null) {
            settingsButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showSettingsDialog();
                }
            });
        }

        if (modelsListView != null) {
            modelsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    HuggingFaceModel model = modelsList.get(position);
                    fetchModelFiles(model);
                }
            });
        }
    }

    private void performSearch() {
        if (searchButton != null) {
            searchButton.setEnabled(false);
        }
        final String query = searchEdit.getText().toString().trim();
        if (TextUtils.isEmpty(query)) {
            statusText.setText("Please enter a search query.");
            if (searchButton != null) {
                searchButton.setEnabled(true);
            }
            return;
        }

        statusText.setText("Searching...");
        setLoadingState(true);

        executor.execute(new Runnable() {
            @Override
            public void run() {
                final SearchResult result = searchModels(query);
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (result.rateLimited) {
                            statusText.setText(result.errorMessage);
                        } else if (result.errorMessage != null) {
                            statusText.setText(result.errorMessage);
                        } else if (result.models.isEmpty()) {
                            statusText.setText("No models found for \"" + query + "\".");
                        } else {
                            modelsList.clear();
                            modelsList.addAll(result.models);
                            modelsAdapter.notifyDataSetChanged();
                            statusText.setText("Found " + result.models.size() + " models.");
                        }
                        setLoadingState(false);
                        if (searchButton != null) {
                            searchButton.setEnabled(true);
                        }
                    }
                });
            }
        });
    }

    private void fetchModelFiles(final HuggingFaceModel model) {
        statusText.setText("Loading files for " + model.getId() + "...");
        setLoadingState(true);

        executor.execute(new Runnable() {
            @Override
            public void run() {
                final ModelDetailResult result = getModelFiles(model.getId());
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (result.rateLimited) {
                            statusText.setText(result.errorMessage);
                        } else if (result.errorMessage != null) {
                            statusText.setText(result.errorMessage);
                        } else if (result.files.isEmpty()) {
                            statusText.setText("No files found for this model.");
                        } else {
                            showFilesDialog(model, result.files);
                            statusText.setText("Showing " + result.files.size() + " files.");
                        }
                        setLoadingState(false);
                    }
                });
            }
        });
    }

    private SearchResult searchModels(String query) {
        SearchResult result = new SearchResult();
        result.models = new ArrayList<HuggingFaceModel>();

        ApiResponse response = makeApiRequest(
            "https://huggingface.co/api/models?search=" + Uri.encode(query) +
            "&sort=downloads&direction=-1&limit=" + SEARCH_LIMIT, "GET", null
        );

        if (response.statusCode == 429) {
            result.rateLimited = true;
            result.errorMessage = formatRateLimitMessage(response);
            return result;
        }

        if (response.statusCode != 200) {
            result.errorMessage = "Error fetching models: HTTP " + response.statusCode;
            return result;
        }

        try {
            JSONArray modelsArray = new JSONArray(response.body);
            List<HuggingFaceModel> models = new ArrayList<HuggingFaceModel>();

            for (int i = 0; i < modelsArray.length(); i++) {
                JSONObject modelObj = modelsArray.getJSONObject(i);

                String modelId = modelObj.optString("modelId", "");
                if (TextUtils.isEmpty(modelId)) {
                    modelId = modelObj.optString("id", "");
                }

                int downloads = modelObj.optInt("downloads", -1);
                String lastModified = modelObj.optString("lastModified", "");
                String pipelineTag = modelObj.optString("pipeline_tag", "");
                String libraryName = modelObj.optString("library_name", "");
                int likes = modelObj.optInt("likes", -1);
                boolean isPrivate = modelObj.optBoolean("private", false);
                boolean isGated = modelObj.optBoolean("gated", false);

                JSONArray tagsArray = modelObj.optJSONArray("tags");
                String tags = "";
                if (tagsArray != null) {
                    StringBuilder tagsBuilder = new StringBuilder();
                    for (int j = 0; j < tagsArray.length(); j++) {
                        if (tagsBuilder.length() > 0) tagsBuilder.append(", ");
                        tagsBuilder.append(tagsArray.optString(j, ""));
                    }
                    tags = tagsBuilder.toString();
                }

                HuggingFaceModel model = new HuggingFaceModel(modelId, downloads, lastModified,
                    tags, pipelineTag, libraryName, likes, isPrivate, isGated);
                models.add(model);
            }
            result.models = models;
        } catch (JSONException e) {
            result.errorMessage = "Error parsing response: " + e.getMessage();
        }

        return result;
    }

    private ModelDetailResult getModelFiles(final String modelId) {
        ModelDetailResult result = new ModelDetailResult();
        result.files = new ArrayList<FileInfo>();

        ApiResponse response = makeApiRequest(
            "https://huggingface.co/api/models/" + modelId + "?blobs=true", "GET", null
        );

        if (response.statusCode == 429) {
            result.rateLimited = true;
            result.errorMessage = formatRateLimitMessage(response);
            return result;
        }

        if (response.statusCode != 200) {
            result.errorMessage = "Error fetching model details: HTTP " + response.statusCode;
            return result;
        }

        try {
            JSONObject modelObj = new JSONObject(response.body);
            JSONArray siblings = modelObj.optJSONArray("siblings");

            if (siblings != null) {
                for (int i = 0; i < siblings.length(); i++) {
                    JSONObject sibling = siblings.getJSONObject(i);
                    String filename = sibling.optString("rfilename", "");
                    if (TextUtils.isEmpty(filename)) {
                        filename = sibling.optString("filename", "");
                    }
                    if (!TextUtils.isEmpty(filename)) {
                        long size = sibling.optLong("size", -1);
                        result.files.add(new FileInfo(filename, size));
                    }
                }
            }
        } catch (JSONException e) {
            result.errorMessage = "Error parsing model details: " + e.getMessage();
        }

        return result;
    }

    private void downloadModelFile(String modelId, String filename) {
        String url = "https://huggingface.co/" + modelId + "/resolve/main/" + filename;
        String destination = Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_DOWNLOADS) + "/hf-models/" + modelId + "/";

        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        request.setTitle("Downloading " + filename);
        request.setDescription("Downloading " + filename + " from " + modelId);
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setDestinationUri(Uri.parse("file://" + destination + filename));
        request.setAllowedOverMetered(true);
        request.setAllowedOverRoaming(true);

        if (!TextUtils.isEmpty(hfToken)) {
            request.addRequestHeader("Authorization", "Bearer " + hfToken);
        }

        DownloadManager downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        if (downloadManager != null) {
            downloadManager.enqueue(request);
            Toast.makeText(this, "Download started: " + filename, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Download service unavailable", Toast.LENGTH_LONG).show();
        }
    }

    private ApiResponse makeApiRequest(String urlString, String method, String body) {
        ApiResponse response = new ApiResponse();
        HttpURLConnection connection = null;

        try {
            URL url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod(method);
            connection.setConnectTimeout(CONNECT_TIMEOUT);
            connection.setReadTimeout(READ_TIMEOUT);
            connection.setRequestProperty("User-Agent", USER_AGENT);
            connection.setRequestProperty("Accept", "application/json");

            if (!TextUtils.isEmpty(hfToken)) {
                connection.setRequestProperty("Authorization", "Bearer " + hfToken);
            }

            if (body != null && (method.equals("POST") || method.equals("PUT"))) {
                connection.setDoOutput(true);
                connection.getOutputStream().write(body.getBytes("UTF-8"));
            }

            response.statusCode = connection.getResponseCode();

            if (response.statusCode == 429) {
                response.retryAfterHeader = connection.getHeaderField("Retry-After");
                response.rateLimitHeader = connection.getHeaderField("X-RateLimit-Remaining");
                response.body = "";
                return response;
            }

            InputStream inputStream;
            if (response.statusCode >= 200 && response.statusCode < 300) {
                inputStream = connection.getInputStream();
            } else {
                inputStream = connection.getErrorStream();
            }

            if (inputStream != null) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
                StringBuilder responseBody = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    responseBody.append(line);
                }
                reader.close();
                response.body = responseBody.toString();
            }

        } catch (IOException e) {
            response.statusCode = -1;
            response.body = "";
            response.rateLimitHeader = null;
            response.retryAfterHeader = null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }

        return response;
    }

    private void setLoadingState(boolean loading) {
        loadingProgress.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    private int getThemeColor(int attr) {
        TypedValue typedValue = new TypedValue();
        getTheme().resolveAttribute(attr, typedValue, true);
        return typedValue.data;
    }

    private String formatRateLimitMessage(ApiResponse response) {
        int waitSeconds = 60;
        if (response.retryAfterHeader != null) {
            try {
                waitSeconds = Integer.parseInt(response.retryAfterHeader);
            } catch (NumberFormatException ignored) {}
        } else if (response.rateLimitHeader != null) {
            try {
                String header = response.rateLimitHeader;
                String[] parts = header.split(";");
                for (String part : parts) {
                    part = part.trim();
                    if (part.startsWith("t=") || part.startsWith("\"t\":")) {
                        String val = part.substring(part.indexOf('=') + 1).replace("\"", "").trim();
                        waitSeconds = Integer.parseInt(val);
                    }
                }
            } catch (Exception ignored) {}
        }
        return "Rate limited by HuggingFace. Please wait " + waitSeconds + " seconds before trying again.";
    }

    private void showFilesDialog(final HuggingFaceModel model, final List<FileInfo> files) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Files - " + model.getId());

        String[] fileArray = new String[files.size()];
        for (int i = 0; i < files.size(); i++) {
            fileArray[i] = files.get(i).toString();
        }

        builder.setItems(fileArray, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                FileInfo selectedFile = files.get(which);
                String filename = selectedFile.getFilename();

                if (model.isGated() && TextUtils.isEmpty(hfToken)) {
                    AlertDialog.Builder tokenWarn = new AlertDialog.Builder(MainActivity.this);
                    tokenWarn.setTitle("Gated Model");
                    tokenWarn.setMessage("This model requires authentication. Set your HuggingFace token in Settings.");
                    tokenWarn.setPositiveButton("Open Settings", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface d, int w) {
                            showSettingsDialog();
                        }
                    });
                    tokenWarn.setNegativeButton("Download Anyway", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface d, int w) {
                            downloadModelFile(model.getId(), filename);
                        }
                    });
                    tokenWarn.show();
                } else {
                    downloadModelFile(model.getId(), filename);
                }
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void showSettingsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Settings");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(24, 16, 24, 16);

        TextView tokenLabel = new TextView(this);
        tokenLabel.setText("HuggingFace Token");
        tokenLabel.setTextSize(14);
        tokenLabel.setTextColor(getThemeColor(android.R.attr.textColorSecondary));
        layout.addView(tokenLabel);

        final EditText tokenInput = new EditText(this);
        tokenInput.setHint("hf_xxx... (optional, for higher rate limits)");
        tokenInput.setInputType(android.text.InputType.TYPE_CLASS_TEXT
                | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        if (!TextUtils.isEmpty(hfToken)) {
            tokenInput.setText(hfToken);
        }
        layout.addView(tokenInput);

        builder.setView(layout);

        builder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String token = tokenInput.getText().toString().trim();
                prefs.edit().putString(PREF_KEY_TOKEN, token).apply();
                hfToken = token;
                Toast.makeText(MainActivity.this,
                        TextUtils.isEmpty(token) ? "Token cleared" : "Token saved",
                        Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private static class SearchResult {
        List<HuggingFaceModel> models;
        String errorMessage;
        boolean rateLimited;
    }

    private static class ModelDetailResult {
        List<FileInfo> files;
        String errorMessage;
        boolean rateLimited;
    }

    private static class ApiResponse {
        int statusCode;
        String body;
        String rateLimitHeader;
        String retryAfterHeader;
    }
}