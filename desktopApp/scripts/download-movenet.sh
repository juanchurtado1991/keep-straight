#!/usr/bin/env bash
# Downloads a MoveNet Lightning ONNX model for KeepStraight desktop.
# Place it under desktopApp/src/main/resources/models/movenet_lightning.onnx
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
OUT_DIR="$ROOT/src/main/resources/models"
OUT_FILE="$OUT_DIR/movenet_lightning.onnx"
mkdir -p "$OUT_DIR"

# MoveNet single-pose Lightning ONNX (Hugging Face / Xenova export).
URL="${MOVENET_ONNX_URL:-https://huggingface.co/Xenova/movenet-singlepose-lightning/resolve/main/onnx/model.onnx}"

echo "Downloading MoveNet Lightning ONNX to $OUT_FILE"
curl -L --fail -o "$OUT_FILE" "$URL"
ls -lh "$OUT_FILE"
echo "Done. Rebuild/run :desktopApp"
