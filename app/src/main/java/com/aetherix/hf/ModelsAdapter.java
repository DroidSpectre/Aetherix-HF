package com.aetherix.hf;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

public class ModelsAdapter extends BaseAdapter {
    private Context context;
    private List<HuggingFaceModel> models;
    private LayoutInflater inflater;

    public ModelsAdapter(Context context, List<HuggingFaceModel> models) {
        this.context = context;
        this.models = models;
        this.inflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() { return models.size(); }

    @Override
    public Object getItem(int position) { return models.get(position); }

    @Override
    public long getItemId(int position) { return position; }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.model_list_item, parent, false);
            holder = new ViewHolder();
            holder.modelName = (TextView) convertView.findViewById(R.id.modelName);
            holder.modelInfo = (TextView) convertView.findViewById(R.id.modelInfo);
            holder.modelTags = (TextView) convertView.findViewById(R.id.modelTags);
            holder.pipelineBadge = (TextView) convertView.findViewById(R.id.pipelineBadge);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        HuggingFaceModel model = models.get(position);
        holder.modelName.setText(model.getId());

        String info = model.getFormattedDownloads() + " downloads";
        if (model.getLikes() > 0) {
            info += "  \u2665 " + model.getLikes();
        }
        holder.modelInfo.setText(info);

        holder.modelTags.setText(model.getTags());

        if (model.hasPipelineTag()) {
            holder.pipelineBadge.setVisibility(View.VISIBLE);
            holder.pipelineBadge.setText(model.getPipelineTag());
            holder.pipelineBadge.setBackgroundColor(getPipelineColor(model.getPipelineTag()));
        } else {
            holder.pipelineBadge.setVisibility(View.GONE);
        }

        return convertView;
    }

    private int getPipelineColor(String tag) {
        if (tag == null) return Color.parseColor("#9E9E9E");
        switch (tag) {
            case "text-generation":
            case "text2text-generation":
                return Color.parseColor("#FFD21E");
            case "image-classification":
            case "image-generation":
            case "text-to-image":
                return Color.parseColor("#FFC107");
            case "automatic-speech-recognition":
            case "text-to-speech":
                return Color.parseColor("#FFB300");
            case "translation":
                return Color.parseColor("#FFA000");
            case "fill-mask":
            case "token-classification":
                return Color.parseColor("#FF8F00");
            case "question-answering":
                return Color.parseColor("#FF6F00");
            case "sentence-similarity":
            case "feature-extraction":
                return Color.parseColor("#FFC107");
            default:
                return Color.parseColor("#FFD21E");
        }
    }

    private static class ViewHolder {
        TextView modelName;
        TextView modelInfo;
        TextView modelTags;
        TextView pipelineBadge;
    }
}