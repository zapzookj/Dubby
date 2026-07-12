# 아이폰 UX 테스트 가이드 (Windows PC + iPhone, Expo Go)

> 대상: 모바일 개발 경험이 없는 오너. PC는 이 레포가 있는 Windows 머신 기준.
> 원리: 아이폰에 **Expo Go**(공식 테스트 러너 앱)를 설치하면, 같은 Wi-Fi에 있는 PC가
> 앱 코드(Metro, 8081 포트)와 백엔드 API(8080 포트)를 아이폰에 실시간으로 서빙한다.
> 앱 수정 → 저장하면 아이폰 화면이 즉시 갱신된다. **Xcode/Mac 불필요.**

```text
[아이폰: Expo Go 앱] ←같은 Wi-Fi→ [PC]
      앱 화면 ←─ 8081 (Metro 번들러)
      API 호출 ←─ 8080 (dubbyServer)
```

---

## 0. 최초 1회 준비 (약 5분)

### 0-1. 아이폰: App Store에서 **Expo Go** 설치 (무료)

### 0-2. PC: 방화벽 포트 열기 (관리자 권한 필요, 최초 1회)
시작 메뉴 → "PowerShell" 검색 → **관리자 권한으로 실행** → 아래 두 줄 붙여넣기:

```powershell
netsh advfirewall firewall add rule name="Dubby Dev Backend 8080" dir=in action=allow protocol=TCP localport=8080
netsh advfirewall firewall add rule name="Dubby Dev Metro 8081" dir=in action=allow protocol=TCP localport=8081
```

둘 다 "확인."이 나오면 성공. (이후로는 이 단계 불필요)

### 0-3. 아이폰과 PC가 **같은 Wi-Fi**에 연결되어 있는지 확인
- PC가 유선랜이어도 같은 공유기면 OK.
- 아이폰이 LTE/5G면 안 됨 — Wi-Fi 켜기.

---

## 1. 백엔드 켜기 (PowerShell 창 ①)

일반 PowerShell 창(관리자 불필요)에서:

```powershell
cd C:\Users\zapza\Desktop\Dubby\dubbyServer
docker compose up -d          # PostgreSQL (Docker Desktop이 켜져 있어야 함)
$env:SPRING_PROFILES_ACTIVE='local'
.\gradlew.bat bootRun
```

1~2분 뒤 로그가 멈추면 기동 완료. **이 창은 켜둔 채로 둔다.**
확인(선택): 브라우저에서 `http://localhost:8080/api/v1/health` → "더비가 살아있는 척에 성공했습니다."

> **채팅 응답에 대해**: 기본값은 **모의 LLM**이라 채팅 답변 끝에 "(모의 응답)"이 붙는다.
> 진짜 더비 페르소나로 테스트하려면 bootRun **전에** 아래를 추가 (OpenRouter 키 필요, 회당 수원 수준):
> ```powershell
> $env:LLM_MOCK='false'
> $env:OPENROUTER_API_KEY='sk-or-여기에-키'
> ```

## 2. 앱 켜기 (PowerShell 창 ② — 새 창)

```powershell
cd C:\Users\zapza\Desktop\Dubby\dubbyMobile
$env:EXPO_PUBLIC_API_BASE_URL='http://192.168.219.101:8080'
npx expo start
```

- `192.168.219.101` = 현재 PC의 Wi-Fi IP. **바뀔 수 있으니** 연결 안 되면 `ipconfig`로
  "무선 LAN 어댑터 Wi-Fi"의 IPv4 주소를 다시 확인해서 넣는다.
- 잠시 후 터미널에 **큰 QR 코드**가 뜬다.
- Windows 보안 경고가 뜨면 "액세스 허용".

## 3. 아이폰 연결

1. 아이폰 **기본 카메라 앱**으로 터미널의 QR 코드를 비춘다.
2. 노란 배너 "Expo Go에서 열기" 탭.
3. 잠시 번들 다운로드 후 → **더비 온보딩 화면**이 뜨면 성공.

---

## 4. 안 될 때 (위에서 막힌 순서대로)

| 증상 | 조치 |
|---|---|
| QR 스캔 후 Expo Go가 무한 로딩 | ① 같은 Wi-Fi인지 재확인 ② 창 ②를 끄고 `npx expo start --tunnel`로 재시작(첫 실행 시 패키지 설치 물어보면 y). 터널 모드는 네트워크 우회라 조금 느리지만 거의 항상 붙는다 |
| 앱은 떴는데 "더비가 서버에게 말을 걸었지만 무시당했습니다" | API(8080)가 안 닿는 것. **아이폰 Safari**에서 `http://192.168.219.101:8080/api/v1/health` 접속 → JSON이 보여야 정상. 안 보이면: ① 0-2 방화벽 재확인 ② `ipconfig`로 IP 재확인 후 창 ②의 주소 수정·재시작 ③ 공유기 "AP 격리/게스트 네트워크"면 → **아이폰 개인용 핫스팟을 켜고 PC를 그 핫스팟 Wi-Fi에 연결** 후 `ipconfig`로 새 IP 확인 (가장 확실한 우회) |
| "SDK 버전 불일치" 류 에러 | App Store에서 Expo Go 업데이트 |
| 화면이 이상하게 캐시됨 | 창 ②에서 Ctrl+C 후 `npx expo start -c` (캐시 초기화) |
| 코드가 수정됐는데 반영 안 됨 | 아이폰에서 앱을 흔들면 개발 메뉴 → Reload |

## 5. 이번 테스트에서 되는 것 / 안 되는 것

**됨:** 온보딩(타이핑) → 홈(상태/정확도/메뉴) → 오늘의 업무(반응 4종·다시 시키기 소진·도감 저장) → 공유(이미지 카드) → 채팅(쿼터 3회·과로 화면·안전 카드·일기 후보) → 일기장(승인/삭제/공유) → 설정(이스터에그 3종·알림 토글·계정 삭제)

**안 됨 (Expo Go 한계 — 예정된 동작):**
- **푸시 실수신**: Expo Go는 원격 푸시 미지원 + EAS 프로젝트 필요. 온보딩의 "허락하기"는 눌러도 조용히 넘어감 (정상)
- **결제**: Paywall에 "스토어 연결 준비 중" 표시 (정상 — RevenueCat 미연결 가드)
- 더비 아바타는 임시 이모지 (PNG 수급 전)

## 6. UX 판정 체크리스트 (오너 게이트 — 이게 이번 테스트의 목적)

각 항목을 "진짜로 불편한가 vs 더비답게 어이없는가"로 판정:

- [ ] 온보딩 30초 안에 "정상 앱이 아니구나"가 전달되는가
- [ ] 홈만 봐도 피식하는가 (상태 문구/정확도)
- [ ] **오늘의 업무 3개가 템플릿만으로 웃긴가** ← 제품 존폐급 질문
- [ ] [혼내기] 등 반응의 후속 멘트가 살아있는가, [다시 시키기] 소진이 개그로 읽히는가
- [ ] 공유 카드 이미지가 SNS에 올리고 싶은 모양인가
- [ ] 채팅 3회 소진 → 과로 화면이 짜증이 아니라 개그인가
- [ ] 일기 후보 카드([적게 두기]/[찢기]) 흐름이 자연스러운가
- [ ] 설정에서 버튼 도망(1회) 후 두 번째 탭이 확실히 동작하는가
- [ ] 로딩 개그가 실제 대기를 더 길게 만들지 않는가
- [ ] 비행기 모드에서 에러 화면 → [다시 시도]로 복구되는가

발견사항은 docs/handover.md 하단이나 GitHub 이슈로 남기면 다음 세션에서 반영한다.

## 7. 종료

- 창 ② (Expo): `Ctrl+C`
- 창 ① (백엔드): `Ctrl+C` → `docker compose stop`
