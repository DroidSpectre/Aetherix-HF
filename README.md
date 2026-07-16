
# Aetherix-HF

Aetherix-HF is an Android Hugging Face model downloader for browsing, searching, and downloading model files directly to a mobile device.  

It supports both anonymous access and authenticated access via `HF_TOKEN`, so users can still use it exactly like the original `hf-downloader` when no token is provided.

## What it does

Aetherix-HF makes it easy to:

- Search Hugging Face models by keyword.
- Browse model metadata such as download counts, tags, and last modified date.
- View available files inside a model repository and choose specific files to download.
- Download model files on Android using the built-in download flow.
- Use authenticated Hugging Face requests when `HF_TOKEN` is set, while continuing to work without any token for public models.

## Why this project exists

This project extends the original `hf-downloader` concept into a mobile-friendly Android app, keeping the same lightweight workflow while adding token support for access to authenticated or gated Hugging Face resources.  

If no token is configured, the app still behaves like the original version and can browse and download public models normally.

## HF_TOKEN support

Aetherix-HF now includes `HF_TOKEN` functions for authenticated Hugging Face API use.

That means the app can send a token when needed for models that require authentication, but the token is optional and not required for standard public downloads.

### Behavior

- `HF_TOKEN` present: authenticated requests are enabled.

- `HF_TOKEN` absent: the app falls back to unauthenticated access and still works for public models, just like the original `hf-downloader`.

## Project structure

From the repository layout, the app is built around these main files:

- `MainActivity.java` for the main UI and app logic .
- `HuggingFaceModel.java` for model metadata handling .
- `ModelsAdapter.java` for rendering search results .
- `FileInfo.java` for file metadata and file listing behavior .
- Android XML layout and resource files for the interface .

## Requirements

- Android device or emulator .
- Internet access for searching and downloading models .
- Optional `HF_TOKEN` for authenticated Hugging Face access .
- No token is needed for public model browsing and downloading.

## Usage

1. Open the app.
2. Search for a model.
3. Select a result.
4. Choose a file to download.
5. If you need authenticated access, configure `HF_TOKEN` before using the app .

## Notes

Aetherix-HF is intended to remain lightweight and practical for mobile use .  
It keeps the original no-key workflow intact while adding optional Hugging Face token support for broader compatibility.

## License

project license here.

***
