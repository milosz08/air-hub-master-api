#!/bin/bash

#
# Copyright (c) 2023 by MILOSZ GILGA <http://miloszgilga.pl>
#
# File name: add-lang.sh
# Last modified: 5/30/23, 4:42 PM
# Project name: air-hub-master-server
#
# Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
# file except in compliance with the License. You may obtain a copy of the License at
#
#     <http://www.apache.org/license/LICENSE-2.0>
#
# Unless required by applicable law or agreed to in writing, software distributed under
# the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS
# OF ANY KIND, either express or implied. See the License for the specific language
# governing permissions and limitations under the license.
#

LANG_DIR="../src/main/resources"
SAMPLE_LANG_FILE_NAME="messages_en.properties"

API_LANG_PREFIX="i18n-api"
JPA_LANG_PREFIX="i18n-jpa"
MAIL_LANG_PREFIX="i18n-mail"

LANG_PREFIXES=("$API_LANG_PREFIX" "$JPA_LANG_PREFIX" "$MAIL_LANG_PREFIX")

ARG_KEY="--lang"
IS_ARG_NOT_EXIST=false

if [ "$#" -eq 0 ]; then
    IS_ARG_NOT_EXIST=true
fi

IFS="=" read -r key value <<< "$@"
if [ "$key" == "$ARG_KEY" ]; then
    OUTPUT_FILE_NAME="messages_$value.properties"
else
    IS_ARG_NOT_EXIST=true
fi

if [ $IS_ARG_NOT_EXIST == true ]; then
    echo "[bash gen script err] <> Available only argument: --lang=<i18n tag>"
    exit 1
fi

for (( i = 0; i < ${#LANG_PREFIXES[@]}; i++ )); do
    BASE_PREFIX=$LANG_DIR/${LANG_PREFIXES[$i]}
    OUTPUT_PATH="$LANG_DIR/${LANG_PREFIXES[$i]}/$OUTPUT_FILE_NAME"

    if [ ! -d "$BASE_PREFIX" ]; then
        echo "[bash gen script err] <> Directory '$BASE_PREFIX' not exist in /resources project context"
        exit 2
    fi
    if [ ! -f "$BASE_PREFIX/$SAMPLE_LANG_FILE_NAME" ]; then
        echo "[bash gen script err] <> Sample file '$SAMPLE_LANG_FILE_NAME' not exist in '$BASE_PREFIX' directory"
        exit 3
    fi
    if [ -f "$OUTPUT_PATH" ]; then
        echo "[bash gen script err] <> Lang file '$OUTPUT_FILE_NAME' already exist in '$BASE_PREFIX' directory"
        exit 4
    fi

    touch "$OUTPUT_PATH"
    {
        echo "# Generated by: ${0##*/}, $(date +"%D %T")";
        echo "# Bash interpreter version: ${BASH_VERSION}";
        echo
        echo -n "# $OUTPUT_FILE_NAME"
    } >> "$OUTPUT_PATH"

    while IFS= read -r line
    do
        if [[ $line =~ ^#.* ]]; then
            continue
        fi
        IFS="=" read -r key value <<< "$line"
        if [ -z "$line" ]; then
            echo "$key" >> "$OUTPUT_PATH"
        else
            echo "$key=" >> "$OUTPUT_PATH"
        fi
    done < "$LANG_DIR/${LANG_PREFIXES[$i]}/$SAMPLE_LANG_FILE_NAME"

    echo "[bash gen script info] <> Output lang file '$OUTPUT_PATH' was successfully generated"
done