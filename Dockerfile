# --- 1단계: 빌드 환경 ---
# Java 21 JDK 이미지를 'builder'라는 별명으로 사용합니다.
FROM eclipse-temurin:21-jdk as builder

# 작업 공간을 /app 으로 만듭니다.
WORKDIR /app

# 현재 폴더의 모든 파일을 Docker 안의 /app 폴더로 복사합니다.
COPY . .

# gradlew 파일에 실행 권한을 부여합니다. (Docker 이미지 내부에서 실행 가능하도록)
RUN chmod +x ./gradlew

# Gradle을 사용해 프로젝트를 빌드합니다.
RUN ./gradlew build


# --- 2단계: 최종 실행 환경 ---
# Java 21 JRE 이미지를 사용합니다. (실행에 필요한 최소한의 요소만 있어 가볍습니다)
FROM eclipse-temurin:21-jre-jammy

# 작업 공간을 /app 으로 만듭니다.
WORKDIR /app

# 1단계(builder)에서 빌드된 결과물(.jar 파일)만 현재 환경으로 가져옵니다.
COPY --from=builder /app/build/libs/*.jar ./app.jar

# 컨테이너가 시작될 때 아래 명령어를 실행합니다.
# prod 프로필을 활성화해서 서버를 실행합니다.
ENTRYPOINT ["java", "-Dspring.profiles.active=prod", "-jar", "app.jar"]