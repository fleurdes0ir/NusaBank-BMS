#!/bin/bash

# NusaBank Runner Script
# Auto-detect JavaFX SDK path dari lokasi umum

# Cari JavaFX SDK secara otomatis
find_javafx() {
    # Lokasi yang umum dipakai
    local candidates=(
        "$HOME/javafx-sdk"
        "$HOME/javafx-sdk-25.0.3"
        "$HOME/Downloads/javafx-sdk"
        "/opt/javafx-sdk"
        "/usr/local/javafx-sdk"
    )

    for path in "${candidates[@]}"; do
        if [ -f "$path/lib/javafx.controls.jar" ]; then
            echo "$path"
            return 0
        fi
    done
    return 1
}

JAVAFX_PATH=$(find_javafx)

if [ -z "$JAVAFX_PATH" ]; then
    echo "ERROR: JavaFX SDK tidak ditemukan."
    echo "Pastikan JavaFX SDK sudah terinstall di salah satu lokasi berikut:"
    echo "  ~/javafx-sdk"
    echo "  ~/javafx-sdk-25.0.3"
    echo "  /opt/javafx-sdk"
    echo ""
    echo "Download JavaFX SDK dari: https://gluonhq.com/products/javafx/"
    exit 1
fi

# Cari project root (folder yang berisi build/classes)
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

if [ ! -d "$SCRIPT_DIR/build/classes" ]; then
    echo "ERROR: Folder build/classes tidak ditemukan."
    echo "Jalankan Build di NetBeans dulu (Shift+F11), lalu coba lagi."
    exit 1
fi

echo "Menggunakan JavaFX dari: $JAVAFX_PATH"
echo "Menjalankan NusaBank..."

cd "$SCRIPT_DIR"
java \
  --module-path "$JAVAFX_PATH/lib" \
  --add-modules javafx.controls,javafx.fxml \
  --enable-native-access=javafx.graphics \
  -Djava.library.path="$JAVAFX_PATH/lib" \
  -cp build/classes \
  banking.Main
