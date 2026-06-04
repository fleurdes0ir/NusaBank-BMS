#!/bin/bash
cd /home/abinbin0s/NetBeansProjects/NusaBank
java \
  --module-path /home/abinbin0s/javafx-sdk/lib \
  --add-modules javafx.controls,javafx.fxml \
  --enable-native-access=javafx.graphics \
  -Djava.library.path=/home/abinbin0s/javafx-sdk/lib \
  -cp build/classes \
  banking.Main
