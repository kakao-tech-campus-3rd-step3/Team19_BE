# ☀️ [충남대 1팀] 무쉼사: 무더위쉼터를 찾는 사람들

### 💡 프로젝트 소개

> 공공데이터 기반으로 전국 무더위쉼터 정보를 수집 및 제공하며, 실시간 날씨, 위치 정보, 사용자 리뷰 및 푸시 알림 기능을 통합하여 사용자가 가장 빠르고 편리하게 가까운 쉼터를 찾도록 돕는 서비스

🔗URL: https://musuimsa-pi.vercel.app/

### 🗓️ 개발 기간

- 2025-08-01 ~ 2025-11-06 (약 12주)

## 🧑‍🤝‍🧑 프로젝트 팀원

### 💻 Frontend

| <img src="https://avatars.githubusercontent.com/u/195718822?v=4" width="100" alt="고은채 프로필"/> | <img src="https://avatars.githubusercontent.com/u/81281798?v=4" width="100" alt="정지원 프로필"/> |
|:---:|:---:|
| [고은채](https://github.com/eunchae-04) | [정지원](https://github.com/jjw5655) |
| FE 테크리더 | 플래너 |

[![Readme Card](https://github-readme-stats.vercel.app/api/pin/?username=kakao-tech-campus-3rd-step3&repo=Team19_FE&theme=github_dark)](https://github.com/kakao-tech-campus-3rd-step3/Team19_FE)

### 💾 Backend

| <img src="https://avatars.githubusercontent.com/u/146078205?v=4" width="100" alt="박수화 프로필"/> | <img src="https://avatars.githubusercontent.com/u/187789828?v=4" width="100" alt="윤아란 프로필"/> | <img src="https://avatars.githubusercontent.com/u/73630653?v=4" width="100" alt="이어진 프로필"/> |
|:---:|:---:|:---:|
| [박수화](https://github.com/hydrationn) | [윤아란](https://github.com/uvrvuoon) | [이어진](https://github.com/win929) |
| 팀장 | 메이커 | BE 테크리더 |

[![Readme Card](https://github-readme-stats.vercel.app/api/pin/?username=kakao-tech-campus-3rd-step3&repo=Team19_BE&theme=github_dark)](https://github.com/kakao-tech-campus-3rd-step3/Team19_BE)

---

## ✨ 주요 기능 상세

| 기능 | 상세 설명 |
| :-------------------------------- | :--- |
| **📍 위치 기반 쉼터 조회** | 현재 위치 기준 반경 1km 내의 가까운 쉼터 목록을 거리순으로 조회하며, 최대 3km 범위 내 모든 쉼터 조회도 가능합니다. |
| **🌎 지도 기반 클러스터링** | 지도 화면의 Zoom 레벨에 따라 광역일 경우 GeoHash 기반 클러스터를, 상세 영역일 경우 개별 쉼터 정보를 제공하여 부하를 줄입니다. (Zoom 13 기준 분기) |
| **🏠 무더위 쉼터 상세 정보 제공** | 쉼터의 주소, 운영 시간, 수용 인원, 냉방 장비(에어컨/선풍기 수) 등 상세 정보를 제공합니다. |
| **🚶 길찾기 및 음성 안내(TTS)** | **TMAP 보행자 경로 API**를 사용해 최적 경로를 계산하고, 경로 안내 문구 기반으로 음성 합성(TTS) 기능을 제공합니다. 경로선과 함께 **15m 이내 접근** 및 **20m 이내 도착** 감지 로직을 통해 실시간 안내를 지원합니다. |
| **♥️ 위시리스트 (찜)** | 마음에 드는 쉼터를 찜하여 나만의 리스트를 만들고, 목록 조회 시 현재 위치와의 거리를 계산하여 보여줍니다. |
| **📝 리뷰 시스템** | 쉼터에 대한 별점(1~5점) 및 내용, 사진 리뷰를 작성/수정/삭제할 수 있습니다. |
| **⭐ 평점 업데이트의 안정성** | 리뷰 작성/수정/삭제 시 **낙관적 락(Optimistic Locking)**과 최대 3회 재시도 로직을 적용하여 쉼터의 총 평점 및 리뷰 개수 집계의 동시성 문제를 해결했습니다. |
| **🖼️ 사진 업로드/보안** | 리뷰 및 프로필 사진 업로드 시 **AWS S3 Presigned URL**을 사용하여 접근 보안 및 비용 효율성을 확보했습니다. |
| **🌡️ 현재 기온 조회** | 사용자의 현재 위치를 기반으로 기상청 초단기실황 API를 통해 현재 기온을 조회합니다. |
| **🔔 폭염 알림 (Push)** | 현재 기온이 35.0°C 이상일 경우 푸시 알림을 발송하며, 알림 간격 50분의 쿨다운을 적용하여 과도한 알림을 방지합니다. |
| **🔔 도착 후 리뷰 알림** | 쉼터 도착 알림을 받으면, 10분 후에 해당 쉼터에 대한 리뷰를 요청하는 푸시 알림을 예약합니다. |
| **🖼️ 쉼터 사진 업데이트** | 쉼터의 위/경도 기반으로 Mapillary API를 연동하여 가장 가까운 스트리트 뷰 이미지를 찾아 사진 URL을 DB에 저장합니다. |
| **⌛ 배치/스케줄러** | **Spring Batch**를 활용하여 공공데이터포털의 쉼터 데이터를 정기적으로 DB에 업데이트하고, 쉼터 위치 변경 시 GeoHash 캐시를 선택적으로 무효화하는 로직을 적용했습니다. |

---

## 💻 사용 기술 (백엔드)

### 언어

![Java 21 LTS](https://img.shields.io/badge/Java%2021%20LTS-007396?style=for-the-badge&logo=Java&logoColor=white)

### 프레임워크

![Spring Boot 3.5.5](https://img.shields.io/badge/Spring%20Boot%203.5.5-6DB33F?style=for-the-badge&logo=Spring%20Boot&logoColor=white)
![Spring Data JPA](https://img.shields.io/badge/Spring%20Data%20JPA-6DB33F?style=for-the-badge&logo=Spring&logoColor=white)
![Spring Security](https://img.shields.io/badge/Spring%20Security-6DB33F?style=for-the-badge&logo=Spring%20Security&logoColor=white)
![Spring Batch](https://img.shields.io/badge/Spring%20Batch-6DB33F?style=for-the-badge&logo=Spring%20Batch&logoColor=white)
![SpringDataJPA](https://img.shields.io/badge/Spring%20Data%20JPA-6DB33F?style=for-the-badge&logo=Spring&logoColor=white)
![JJWT](https://img.shields.io/badge/JJWT-black?style=for-the-badge&logo=JSON%20web%20tokens)

### 데이터베이스

![MySQL 8](http://img.shields.io/badge/MySQL-4479A1?style=for-the-badge&logo=MySQL&logoColor=white)
![H2 Database](https://img.shields.io/badge/h2%20database-09476B?style=for-the-badge&logo=h2database&logoColor=white)

### 캐시

![Redis](https://img.shields.io/badge/Redis-DC382D?style=for-the-badge&logo=Redis&logoColor=white)
![Caffeine Cache](https://img.shields.io/badge/Caffeine%20Cache-D74A25?style=for-the-badge&logo=data:image/svg+xml;base64,PHN2ZyByb2xlPSJpbWciIHhtbG5zPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwL3N2ZyIgdmlld0JveD0iMCAwIDI0IDI0Ij48cGF0aCBkPSJNMjEgMTBIMjlWMjRINjVWMjkuNUgxMjlWMzguNUgxNDZWMTAzLjlIMTU1LjVWMjA4LjVIMTY0LjlWMjI0LjVIMTI5VjI3Ni41SDEwNC43VjMzNC41SDM3VjMwNy41SDBWMjM4LjVINTBWMTU5LjVIMTcuMkwxNi45IDEzOS44QzE0LjkgMTE0LjQgMjEgMTAwIDIxIDEwWiIgZmlsbD0iI0Q3NGEyNSIvPjwvc3ZnPg==&logoColor=white)

### 외부 API

![Firebase FCM](https://img.shields.io/badge/Firebase%20FCM-FFCA28?style=for-the-badge&logo=firebase&logoColor=black)
![행정안전부](https://img.shields.io/badge/행정안전부-003764?style=for-the-badge&logo=data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSI2MDAiIGhlaWdodD0iNjAwIiB2aWV3Qm94PSIwIDAgMTczLjI4MiAxNzMuMjgyIj48cGF0aCBmaWxsPSIjZmZmIiBkPSJNMTczLjI4MiA4Ni42NWMwIDQ3Ljg0NC0zOC43OTQgODYuNjMyLTg2LjY0NiA4Ni42MzJDMzguNzk5IDE3My4yODIgMCAxMzQuNDk0IDAgODYuNjUgMCAzOC43ODggMzguNzk5IDAgODYuNjM2IDBjNDcuODUyIDAgODYuNjQ2IDM4Ljc4OCA4Ni42NDYgODYuNjUiLz48cGF0aCBmaWxsPSIjMDAzNzY0IiBkPSJNMTI3LjM4OSA4MC41OThjLTEzLjc5MS05LjM2NS0zMS40MzktNS41NDItNDAuNzE3IDguNTMzLTcuNzIgMTEuNzY3LTE5LjQwNSAxMy4yMzUtMjMuOTA2IDEzLjIzNS0xNC43NTEgMC0yNC44MjQtMTAuMzY3LTI3LjgxOC0yMS4wOTVoLS4wMWMtLjAzOS0uMTA4LS4wNTktLjItLjA5LS4zMDctLjAyNS0uMTItLjA1Ni0uMjMzLS4wODktLjM2Ni0xLjE3Ny00LjQ2Ny0xLjQ2Ny02LjYwOS0xLjQ2Ny0xMS4zNjggMC0yNS42NSAyNi4zMjEtNTQuMjEzIDY0LjIxNS01NC4yMTMgMzguODI5IDAgNjEuMDUgMjkuNTQ1IDY2Ljc4IDQ1Ljk3OS0uMTA3LS4yOTUtLjIwOS0uNTgxLS4yOTEtLjg3Ny0xMS4wMTUtMzIuMTI1LTQxLjQ3NC01NS4yMTYtNzcuMzU3LTU1LjIxNi00NS4xMyAwLTgxLjcyOSAzNi41OS04MS43MjkgODEuNzM5IDAgNDAuMzUxIDI5LjEwOCA3NC44OTEgNjkuNDc5IDc0Ljg5MSAzMi4xOTcgMCA1My44NDItMTguMDUyIDYzLjc1Ny00Mi45MyA1LjQ1LTEzLjYxNCAxLjU5NS0yOS42MDUtMTAuNzU3LTM4LjAwNSIvPjxwYXRoIGZpbGw9IiNlNDAzMmUiIGQ9Ik0xNjQuNzg4IDYyLjU4OWMtNC43NzctMTYuMDI2LTI3LjE1My00Ny41NzEtNjcuMjgyLTQ3LjU3MS0zNy44OTQgMC02NC4yMTQgMjguNTYzLTY0LjIxNCA1NC4yMTIgMCA0Ljc1OS4yOSA2LjkwMSAxLjQ2NiAxMS4zNjgtLjQ4OS0xLjk1MS0uNzQtMy45MDgtLjc0LTUuODIzIDAtMjYuNzIxIDI2Ljc0MS00NS4yMjcgNTQuMjQ4LTQ1LjIyNyAzNy4yMTggMCA2Ny4zODggMzAuMTc0IDY3LjM4OCA2Ny4zOSAwIDI5LjE3My0xNi43ODUgNTQuNDM3LTQxLjE3OSA2Ni41NjV2LjAyM2MzMS40NTUtMTEuMzkxIDUzLjkwOC00MS41MTIgNTMuOTA4LTc2Ljg4NCAwLTguMzc4LTEuMTI3LTE1Ljc1OS0zLjU5NS0yNC4wNTMiLz48L3N2Zz4=&logoColor=white)
![KMA API (기상청)](https://img.shields.io/badge/기상청%20API-003764?style=for-the-badge&logo=data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSI2MDAiIGhlaWdodD0iNjAwIiB2aWV3Qm94PSIwIDAgMTczLjI4MiAxNzMuMjgyIj48cGF0aCBmaWxsPSIjZmZmIiBkPSJNMTczLjI4MiA4Ni42NWMwIDQ3Ljg0NC0zOC43OTQgODYuNjMyLTg2LjY0NiA4Ni42MzJDMzguNzk5IDE3My4yODIgMCAxMzQuNDk0IDAgODYuNjUgMCAzOC43ODggMzguNzk5IDAgODYuNjM2IDBjNDcuODUyIDAgODYuNjQ2IDM4Ljc4OCA4Ni42NDYgODYuNjUiLz48cGF0aCBmaWxsPSIjMDAzNzY0IiBkPSJNMTI3LjM4OSA4MC41OThjLTEzLjc5MS05LjM2NS0zMS40MzktNS41NDItNDAuNzE3IDguNTMzLTcuNzIgMTEuNzY3LTE5LjQwNSAxMy4yMzUtMjMuOTA2IDEzLjIzNS0xNC43NTEgMC0yNC44MjQtMTAuMzY3LTI3LjgxOC0yMS4wOTVoLS4wMWMtLjAzOS0uMTA4LS4wNTktLjItLjA5LS4zMDctLjAyNS0uMTItLjA1Ni0uMjMzLS4wODktLjM2Ni0xLjE3Ny00LjQ2Ny0xLjQ2Ny02LjYwOS0xLjQ2Ny0xMS4zNjggMC0yNS42NSAyNi4zMjEtNTQuMjEzIDY0LjIxNS01NC4yMTMgMzguODI5IDAgNjEuMDUgMjkuNTQ1IDY2Ljc4IDQ1Ljk3OS0uMTA3LS4yOTUtLjIwOS0uNTgxLS4yOTEtLjg3Ny0xMS4wMTUtMzIuMTI1LTQxLjQ3NC01NS4yMTYtNzcuMzU3LTU1LjIxNi00NS4xMyAwLTgxLjcyOSAzNi41OS04MS43MjkgODEuNzM5IDAgNDAuMzUxIDI5LjEwOCA3NC44OTEgNjkuNDc5IDc0Ljg5MSAzMi4xOTcgMCA1My44NDItMTguMDUyIDYzLjc1Ny00Mi45MyA1LjQ1LTEzLjYxNCAxLjU5NS0yOS42MDUtMTAuNzU3LTM4LjAwNSIvPjxwYXRoIGZpbGw9IiNlNDAzMmUiIGQ9Ik0xNjQuNzg4IDYyLjU4OWMtNC43NzctMTYuMDI2LTI3LjE1My00Ny41NzEtNjcuMjgyLTQ3LjU3MS0zNy44OTQgMC02NC4yMTQgMjguNTYzLTY0LjIxNCA1NC4yMTIgMCA0Ljc1OS4yOSA2LjkwMSAxLjQ2NiAxMS4zNjgtLjQ4OS0xLjk1MS0uNzQtMy45MDgtLjc0LTUuODIzIDAtMjYuNzIxIDI2Ljc0MS00NS4yMjcgNTQuMjQ4LTQ1LjIyNyAzNy4yMTggMCA2Ny4zODggMzAuMTc0IDY3LjM4OCA2Ny4zOSAwIDI5LjE3My0xNi43ODUgNTQuNDM3LTQxLjE3OSA2Ni41NjV2LjAyM2MzMS40NTUtMTEuMzkxIDUzLjkwOC00MS41MTIgNTMuOTA4LTc2Ljg4NCAwLTguMzc4LTEuMTI3LTE1Ljc1OS0zLjU5NS0yNC4wNTMiLz48L3N2Zz4=&logoColor=white)
![Mapillary](https://img.shields.io/badge/Mapillary-263238?style=for-the-badge&logo=openstreetmap&logoColor=white)

### 개발 도구

![IntelliJ IDEA](https://img.shields.io/badge/IntelliJ_IDEA-000000.svg?style=for-the-badge&logo=intellij-idea&logoColor=white)
![Gradle](https://img.shields.io/badge/Gradle-02303A?style=for-the-badge&logo=gradle&logoColor=white)
![Git](https://img.shields.io/badge/Git-F05032?style=for-the-badge&logo=Git&logoColor=white)
![GitHub](https://img.shields.io/badge/GitHub-181717?style=for-the-badge&logo=GitHub&logoColor=white)

### CI/CD
![Github Actions](https://img.shields.io/badge/Github%20Actions-2088FF?style=for-the-badge&logo=githubactions&logoColor=white)
![AWS](https://img.shields.io/badge/AWS-232F3E?style=for-the-badge&logo=amazon-aws&logoColor=white)
![AWS S3](https://img.shields.io/badge/AWS%20S3-FF9900?style=for-the-badge&logo=amazon-s3&logoColor=white)

### 테스팅

![JUnit5](https://img.shields.io/badge/JUnit5-25A162?style=for-the-badge&logo=Junit5&logoColor=white)
![Mockito](https://img.shields.io/badge/Mockito-88B35A?style=for-the-badge&logo=mockito&logoColor=white)
![AssertJ](https://img.shields.io/badge/AssertJ-black?style=for-the-badge&logo=git&logoColor=white)
![MockWebServer](https://img.shields.io/badge/MockWebServer-000000?style=for-the-badge&logo=okhttp&logoColor=white)
![S3Mock](https://img.shields.io/badge/S3Mock-blue?style=for-the-badge&logo=amazon-s3&logoColor=white)

### 문서

![Swagger (OpenAPI)](https://img.shields.io/badge/Swagger-85EA2D?style=for-the-badge&logo=swagger&logoColor=black)

### 협업 도구

![Discord](https://img.shields.io/badge/Discord-5865F2?style=for-the-badge&logo=discord&logoColor=white)
![Notion](https://img.shields.io/badge/Notion-000000?style=for-the-badge&logo=notion&logoColor=white)
![Figma](https://img.shields.io/badge/Figma-F24E1E?style=for-the-badge&logo=figma&logoColor=white)

---

## 🏗️ 아키텍처 구조

![Image](https://github.com/user-attachments/assets/54e2315e-bd94-44b6-b1b8-431088847e8d)
<br>

## 🚀 CI/CD

![Image](https://github.com/user-attachments/assets/7e139b5f-e5dd-4c49-9cc4-fbe63c36dbec)

<br>

### 🗄️ ERD

![image](https://github.com/user-attachments/assets/793ec7b7-87bc-4225-9cdc-dbab82cef49a)

<br>

# ✨ 기능

### 👤 박수화

쉼터(Shelter) 및 지도 검색 핵심 로직 구현

- Shelter API: 쉼터 정보 조회 및 상세 정보 제공 컨트롤러/서비스 구현

- 지도 검색: GeoHash 기반의 클러스터링 로직을 포함한 지도 경계 기반 쉼터 검색 서비스 구현

- 위시리스트 (Wish): 쉼터 찜하기/취소 및 목록 조회 컨트롤러/서비스 구현

- 쉼터 사진: Mapillary API를 연동하여 쉼터 위치 기반 스트리트 뷰 사진을 가져오고 URL을 업데이트하는 서비스 구현

- S3 Presigned URL: 사진 업로드를 위한 S3 Presigned URL 생성 서비스 구현

- 평점 안정성: 리뷰 변경 시 쉼터 평점 업데이트에 낙관적 락(Optimistic Locking)을 적용하는 로직 구현

### 👤 윤아란

리뷰 및 날씨 정보 시스템 구현

- 리뷰 (Review) CRUD: 리뷰 작성, 수정, 삭제 컨트롤러/서비스 구현

- Caffeine 캐시 사용
  
- Jpa Auditing 적용

- 날씨 API: 사용자의 현재 위치 기반으로 기상청 API를 호출하여 기온을 조회하는 서비스 구현

### 👤 이어진

인증/인가, 스케줄러/배치, CI/CD 및 AWS 인프라 구축

- 인증/인가: JWT 기반 로그인, 회원가입, 토큰 갱신 컨트롤러/서비스 및 Spring Security 설정, JWT 필터 구현

- CI/CD 파이프라인: GitHub Actions 기반 Docker 이미지 빌드 및 AWS ECR/EC2 자동 배포 로직 구축.

- 스케줄러/알림: Spring Batch(쉼터 데이터 업데이트), FCM을 이용한 폭염 알림 및 리뷰 리마인더 예약/발송 구현.

### 👥 팀원 공통

- Database ERD 설계
- Test code 작성

## 📚 API 문서 (명세)

- 📝 [노션 URL](https://www.notion.so/teamsparta/2442dc3ef51481e18ce2dbe20fcd08e4?source=copy_link#2532dc3ef51480e9ba0bfd38a6c1c9d0)
- 🔍 [Swagger](http://api.musuimsa.kro.kr/swagger-ui/index.html)
