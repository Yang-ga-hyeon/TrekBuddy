import os
import json


# 텍스트 파일 읽기
with open('PlaceTitle.txt', 'r', encoding='utf-8') as txt_file:
    # 각 줄을 리스트로 읽어오기
    names_list = [line.strip() for line in txt_file]

# 데이터 구조 생성
data = {
    "places": [
        {"name": name, "id": None, "passage": [], "geo": [], "summary": [], "script": []} for name in names_list
    ]
}

# 각 name에 해당하는 titlesrc 폴더 내의 텍스트 파일을 읽어와서 passage와 geo에 추가
for place in data["places"]:
    passage_file_path = os.path.join('titlesrc', f'{place["name"]}.txt')
    geo_file_path = os.path.join(f'{place["name"]}.txt')
    print(place["name"])
    try:
        with open(passage_file_path, 'r', encoding='utf-8') as passage_file:
            # 텍스트 파일 내용을 읽어와서 각 줄을 리스트로 추가
            lines = [line.strip() for line in passage_file]
            place["passage"].extend(lines)
    except FileNotFoundError:
        print(f"File {passage_file_path} not found.")

    try:
        with open(geo_file_path, 'r', encoding='utf-8') as geo_file:
            # 텍스트 파일 내용을 읽어와서 각 줄을 정수형으로 변환하여 geo에 추가
            lines = [line.strip() for line in geo_file]
            place["geo"].extend([float(num) for num in lines])
    except FileNotFoundError:
        print(f"File {geo_file_path} not found.")

# 딕셔너리를 JSON 문자열로 변환
json_data = json.dumps(data, indent=2, ensure_ascii=False)

# JSON 파일로 저장
with open('places.json', 'w', encoding='utf-8') as json_file:
    json_file.write(json_data)