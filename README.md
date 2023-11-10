# TrekBuddy
<img width="266" alt="logo2" src="https://github.com/Yang-ga-hyeon/TrekBuddy/assets/111068038/6d7fc7fa-8231-4d3b-904a-4571d6421aff">

### 프로젝트 소개
한국어 텍스트 모델 기반 여행 가이드 어플리케이션입니다.

## ⏲개발 기간
* 2023.08.01 ~ 2023.11.10

### 🧑‍🤝‍🧑멤버
- 조수빈: Project Leader, ML Data Collection & Preprocess, App Develop, Database Management, 가이드 페이지, 검색 페이지, 스크랩 페이지
- 양가현: Main App Developer, Database Management, 로그인 페이지, 회원가입 페이지, 가이드 페이지, 가이드 상세 페이지
- 김윤선: Data Preprocess & Model Finetuning, App Develop, Database Management, 가이드 페이지, 개인정보 수정 페이지, 리뷰 페이지, 로그 페이지

### 💻개발 환경
- `Kotlin` <img src="https://img.shields.io/badge/kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white">
- `Android studio Giraffe | 2022.3.1 Patch 2` <img src="https://img.shields.io/badge/android-3DDC84?style=for-the-badge&logo=android&logoColor=white">
- **Pretrained-model** : 한국어 Bart 모델 오픈 소스 (https://github.com/SKT-AI/KoBART) <img src="https://img.shields.io/badge/google colab-F9AB00?style=for-the-badge&logo=google colab&logoColor=white">
- **Training data** : AI hub 요약문 및 레포트 생성 데이터 (https://aihub.or.kr/aihubdata/data/view.do?currMenu=115&topMenu=100&aihubDataSe=realm&dataSetSn=582) <img width="100" height="20" src="https://github.com/Yang-ga-hyeon/TrekBuddy/assets/111068038/f4264824-e151-4a5b-94ea-c257113cf0b8">

- **Deep Learning Framework** :<img src="https://img.shields.io/badge/PyTorch-EE4C2C?style=for-the-badge&logo=PyTorch&logoColor=white"> <img src="https://img.shields.io/badge/Python-3776AB?style=for-the-badge&logo=Python&logoColor=white">
- **TTS** : NAVER CLOVA <img width="50" height="50" src="https://github.com/Yang-ga-hyeon/TrekBuddy/assets/111068038/9af716e9-dd8c-411c-bffb-7ab7c03a04b3">
- **Database** : <img src="https://img.shields.io/badge/Firebase-FFCA28?style=for-the-badge&logo=Firebase&logoColor=white">

## 아키텍처
<img width="1017" alt="아키텍처" src="https://github.com/Yang-ga-hyeon/TrekBuddy/assets/111068038/2f32b512-d40b-4d81-9c86-66f3da123b4d"> <br/>

<img width="1348" alt="아키텍처-기술" src="https://github.com/Yang-ga-hyeon/TrekBuddy/assets/111068038/3fe28e87-8341-44bc-86b0-77526c2b430d">
  
## 🌟주요 기능
**1. 관광지별 가이드** <br/>주변에 있는 코스를 둘러 보고 마음에 드는 코스를 클릭, 관광지 핀을 선택할 수 있습니다.
  <br/> ① 사용자가 관광지 근처에 있으면 버튼이 활성화되어 가이드를 들을 수 있고
  <br/> ② 그렇지 않으면 구글 길찾기로 관광지까지 가는 길을 검색할 수 있습니다.

**2. 코스 검색** <br/> 등록된 코스를 태그 & 관광지 이름으로 검색하세요. 검색 결과에서 코스를 스크랩하고 가이드를 청취할 수 있습니다. 

**3. 코스 스크랩 모아보기** <br/> 검색 결과에서 나중에 다시 보고 싶은 코스는 스크랩해두고 마이 페이지에서 모아 보세요.

**4. 방문 코스 공유** <br/> 하루동안 방문한 관광지와 코스를 모아보세요. 방문한 코스는 코스를 커스텀하여 다른 사용자에게 공유할 수 있습니다.

## 📆To do
- [ ] 검색 결과 코스 디자인과 시스템 등록 코스 디자인 통일
- [ ] 검색 시 검색어가 포함되는 모든 검색 표시
- [ ] 사용자 하루 방문 코스 기준 시간 설정 기능 추가
- [ ] start 버튼 여부 관계 없이 관광지를 돌아다니며 자동으로 가이드를 실행하는 옵션 추가
      





