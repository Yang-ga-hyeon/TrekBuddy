import firebase_admin
import json
import os
from firebase_admin import credentials, storage, initialize_app

cred = credentials.Certificate("serviceKey.json")
firebase_admin.initialize_app(cred, {'storageBucket': "android-app-12come.appspot.com"})

with open('PlaceTitle.txt', 'r', encoding='utf-8') as txt_file:
    names_list = [line.strip() for line in txt_file]

with open('places_summary.json', 'r', encoding='utf-8') as json_file:
    data = json.load(json_file)

bucket = storage.bucket()

for name in names_list:
    file_name = f"{name}.script.txt"
    for item in data["places"]:
        if item["name"]==name:
            script_content = item["script"]
            # 파일 업로드
            blob = bucket.blob(f"scripts/{file_name}")
            blob.upload_from_string(script_content, content_type='text/plain')

    print(f"Script '{file_name}' uploaded successfully.")