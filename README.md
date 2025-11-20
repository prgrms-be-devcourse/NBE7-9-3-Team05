# 🏋️ Together Motionit

> 혼자서는 포기하기 쉬운 홈트레이닝을
같은 영상으로 동료들과 함께 운동하며 도전하고,
진행 상황을 공유하고 서로 응원하며
운동 습관을 형성해 나가는 AI & 소셜 운동 챌린지 서비스
>
> ※ 본 프로젝트는 Java 기반에서 Kotlin 기반으로 전환되었습니다.
> 
---

## 📌 프로젝트 개요

* **프로젝트 이름:** Together Motionit
* **한줄 요약:**  AI와 소셜 기능을 결합한 홈트 챌린지 플랫폼


## 🛠️ 기술 스택
<img src="https://img.shields.io/badge/Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white"/> <img src="https://img.shields.io/badge/JaCoCo-C41E3A?style=for-the-badge&logo=codecov&logoColor=white"/> <img src="https://img.shields.io/badge/Blue--Green Deployment-1E90FF?style=for-the-badge&logo=azurepipelines&logoColor=white"/> <img src="https://img.shields.io/badge/Grafana-F46800?style=for-the-badge&logo=grafana&logoColor=white"/> <img src="https://img.shields.io/badge/k6-7D64FF?style=for-the-badge&logo=k6&logoColor=white"/> <img src="https://img.shields.io/badge/Vercel-000000?style=for-the-badge&logo=vercel&logoColor=white"/> <img src="https://img.shields.io/badge/Spring Boot-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white"/> <img src="https://img.shields.io/badge/MySQL-4479A1?style=for-the-badge&logo=mysql&logoColor=white"/> <img src="https://img.shields.io/badge/WebSocket-010101?style=for-the-badge&logo=socketdotio&logoColor=white"/> <img src="https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white"/> <img src="https://img.shields.io/badge/Cloud SQL-4285F4?style=for-the-badge&logo=googlecloud&logoColor=white"/> <img src="https://img.shields.io/badge/AWS EC2-FF9900?style=for-the-badge&logo=amazonaws&logoColor=white"/> <img src="https://img.shields.io/badge/AWS S3-569A31?style=for-the-badge&logo=amazons3&logoColor=white"/> 

<img src="https://img.shields.io/badge/Next.js-000000?style=for-the-badge&logo=nextdotjs&logoColor=white"/>

<img src="https://img.shields.io/badge/JUnit5-25A162?style=for-the-badge&logo=junit5&logoColor=white"/> <img src="https://img.shields.io/badge/OpenAPI-6BA539?style=for-the-badge&logo=openapiinitiative&logoColor=white"/> <img src="https://img.shields.io/badge/YouTube Data API-FF0000?style=for-the-badge&logo=youtube&logoColor=white"/> <img src="https://img.shields.io/badge/Swagger-85EA2D?style=for-the-badge&logo=swagger&logoColor=black"/>

---

## 👥 팀원 소개

| <img src="https://github.com/gksdud1109.png" width="120px;" alt=""/> | <img src="https://github.com/BE9-KNH.png" width="120px;" alt=""/> | <img src="https://github.com/lambsteak-dev.png" width="120px;" alt=""/> | <img src="https://github.com/minibr.png" width="120px;" alt=""/> | <img src="https://github.com/LeeMinwoo115.png" width="120px;" alt=""/> | <img src="https://github.com/heygeeji.png" width="120px;" alt=""/> |
| :----------------------------------------------------: | :----------------------------------------------------: | :----------------------------------------------------: | :----------------------------------------------------: | :----------------------------------------------------: | :----------------------------------------------------: |
| **[정한영](https://github.com/gksdud1109)** | **[김나현](https://github.com/BE9-KNH)** | **[김현수](https://github.com/lambsteak-dev)** | **[박민형](https://github.com/minibr)** | **[이민우](https://github.com/LeeMinwoo115)** | **[이혜지](https://github.com/heygeeji)** |
| `FE·BE` | `FE·BE` | `FE·BE` | `FE·BE` | `FE·BE` | `FE·BE` |
| 팀장<br/>JPA Entity 설계<br/>운동방 참여/탈퇴<br/>미션 영상 게시<br/>YouTube API 연동<br/>k6 테스트 구현 및 시행<br/>프로젝트 발표 | 좋아요 기능 구현<br/>중복 방지 로직 | 댓글 CRUD<br/>욕설 필터링 구현 | 공통 클래스 설계<br/>JWT 로그인<br/>AI 메시지<br/>내정보 페이지<br/>Blue Green 무중단 배포 | 운동방 CRUD<br/>WebSocket 처리<br/> | 소셜로그인<br/>(OAuth2.0)<br/>인증/인가<br/>토큰 갱신 로직 |

---

## 🧩 주요 기능

*  운동방 생성 및 관리
*  방 참여 및 목록/검색 기능
*  챌린지 참여자 관리 로직
*  댓글 작성/조회/수정/삭제(욕설 필터링 포함)
*  댓글 좋아요 기능
*  JWT 인증 및 권한 분리, OAuth2 소셜 로그인, Access Token 재발급 로직
*  YouTube Data API 연동
*  AI 응원 메시지 자동 생성(OpenAI API 연동)
*  WebSocket 실시간 기능
*  Blue Green 무중단 배포
*  Jacoco 코드 커버리지 측정


---

## 📝 유저 스토리

<details>
<summary><b>🏠 운동방 관리 (R-1 ~ R-5, R-10)</b></summary>

<br/>

- **R-1 [운동방 개설]**
  - 유저는 운동방을 개설할 수 있다.
  - 유튜브 운동 영상 첨부
  - 참여 인원 제한
  - 카테고리 설정(홈트, 요가 등)
  - 제목, 설명
  - 운동 기간 설정

- **R-2 [운동방 삭제]**
  - 유저는 자신이 개설한 운동방을 삭제할 수 있다.

- **R-3 [운동방 조회]**
  - 유저는 현재 개설된 모든 운동방을 조회할 수 있다.

- **R-4 [운동방 참여]**
  - 유저는 운동방 정원이 남아 있을 때 운동방에 참여할 수 있다.

- **R-5 [운동방 참가자 목록 조회]**
  - 방 참여자는 운동방 내 참가자 목록을 조회할 수 있다.

- **R-10 [운동방 탈퇴]**
  - 방 참여자는 운동방을 탈퇴할 수 있다.

</details>

<details>
<summary><b>🎯 미션 관리 (R-6 ~ R-9)</b></summary>

<br/>

- **R-6 [유튜브 영상 첨부]**
  - 방 참여자는 일일미션(유튜브 영상)을 게시할 수 있다.

- **R-7 [일일 미션 완료]**
  - 방 참여자는 일일미션 완료 표시를 할 수 있다.

- **R-8 [미션 완료 여부 조회]**
  - 방 참여자는 다른 참여자들의 일일 미션 완료 여부를 확인할 수 있다.

</details>

<details>
<summary><b>💬 소셜 기능 (M-1 ~ M-3, L-1)</b></summary>

<br/>

- **M-1 [댓글 조회]**
  - 방 참여자는 운동방에 적힌 댓글을 조회할 수 있다.

- **M-2 [댓글 작성]**
  - 방 참여자는 운동방에 댓글을 작성할 수 있다.

- **M-3 [댓글 수정/삭제]**
  - 방 참여자는 자신이 작성한 댓글을 수정, 삭제할 수 있다.

- **L-1 [댓글 좋아요]**
  - 방 참여자는 댓글에 좋아요를 할 수 있다.

</details>

<details>
<summary><b>👤 사용자 관리 (U-1 ~ U-3)</b></summary>

<br/>

- **U-1 [로그인/회원가입]**
  - 유저는 로그인/회원가입을 통해 서비스 이용 자격을 얻을 수 있다.

- **U-2 [내 정보 조회]**
  - 유저는 로그인 후 자신의 정보를 조회할 수 있다.

- **U-3 [정보 수정]**
  - 유저는 자신의 정보를 수정할 수 있다.
  - 프로필 이미지 업로드
  - 닉네임, 비밀번호 변경

</details>

---

## 🛠 아키텍처

<details>
<summary><b>아키텍처 다이어그램 보기</b></summary>

<br/>
<img width="2033" height="1373" alt="image" src="https://github.com/user-attachments/assets/e75ce853-2d46-4d87-b23a-cfe3d3e3c98c" />

</details>

---

## 🗄️ ERD

<details>
<summary><b>ERD 다이어그램 보기</b></summary>

<br/>

<img width="1656" height="1177" alt="image" src="https://github.com/user-attachments/assets/b46ebdbd-512b-4213-ac16-a1a6d0b99bd2" />

</details>

---

## 📘 API 명세

<details>
<summary><b>API 명세서 링크</b></summary>

<br/>

[📄 API 명세서 바로가기](https://www.notion.so/API-28a9d0051b998056bccecd0cfd988b24)

</details>

---

## 🚀 Blue–Green 무중단 배포

GitHub Actions → Docker → EC2 환경에서  
Blue–Green 전략을 이용한 무중단 배포

- Blue / Green 두 컨테이너를 번갈아 가동
- 신규 버전 헬스체크 성공 시 NGINX가 트래픽 전환
- 장애 시 이전 버전으로 즉시 롤백 가능

---

## 🧪 JaCoCo 코드 커버리지 측정

PR 생성 시 GitHub Actions가 자동으로 테스트를 실행  
JaCoCo 커버리지를 계산해 PR에 댓글로 출력

- Line / Branch Coverage 자동 측정  
- 테스트 누락 구간 시각화  
- 코드 품질 관리에 활용


---

## 🧭 개발 컨벤션

### 브랜치 전략

| 브랜치 | 설명 | 예시 |
|--------|------|------|
| `main` | 프로덕션 배포 브랜치 | `main` |
| `feat/*` | 기능 단위 작업 브랜치 | `feat/login-api` |
| `fix/*` | 버그 수정 브랜치 | `fix/token-expire` |
| `refactor/*` | 리팩토링 작업 브랜치 | `refactor/service-layer` |

**작업 흐름**
1. **GitHub Issue 생성** - 작업 내용 정의
2. **브랜치 생성** - `main`에서 작업 브랜치 생성
3. **개발 작업** - 기능 구현 및 커밋
4. **Pull Request** - `main`으로 PR 생성
5. **코드 리뷰** - 팀원 리뷰 및 피드백
6. **Merge** - 승인 후 `main`에 병합


### 커밋 컨벤션
```
feat:     새로운 기능 추가
fix:      버그 수정
refactor: 코드 리팩토링
docs:     문서 수정
style:    코드 포맷팅
test:     테스트 코드
chore:    빌드 작업, 패키지 관리
```

**커밋 메시지 예시**
```
feat: 운동방 생성 API 구현
fix: JWT 토큰 만료 오류 수정
refactor: 댓글 서비스 로직 개선
```








