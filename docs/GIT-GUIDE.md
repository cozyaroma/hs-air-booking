# GitHub로 함께 작업하기

평가 항목 "개발 과정·협업"은 GitHub 이력과 AI 활용 기록을 봅니다.
세 명이 **각자 자기 폴더에서, 각자 브랜치로** 작업하면 충돌이 거의 나지 않습니다.

## 1. 처음 한 번 (통합 담당)

```bash
cd airline-booking
git init
git add .
git commit -m "[공용] 팀 작업용 베이스 (로그인, 왕복, 페이지 계약, 테스트 도구)"
git branch -M main
git remote add origin https://github.com/<계정>/<저장소>.git
git push -u origin main
```

- GitHub 저장소 Settings → Collaborators에서 두 사람 초대
- (권장) Settings → Branches → `main` 보호: Pull Request로만 합치기, 승인 1명 필요
- Issues에 라벨 `공용변경`, `A`, `B`, `C`, `버그` 만들기

## 2. 처음 한 번 (A, B, C 각자)

```bash
git clone https://github.com/<계정>/<저장소>.git
cd <저장소>
test.bat            # 52개 통과 확인 (macOS/Linux: ./test.sh)
```

IntelliJ를 쓰면: `File → Open`으로 폴더를 열고, `src`를 Sources Root, `test`를 Test Sources Root로 지정합니다.
(`.idea/`는 `.gitignore`에 들어 있어서 각자 설정이 섞이지 않습니다.)

## 3. 기능 하나를 만들 때마다

```bash
git switch main
git pull                                   # 다른 사람 작업 받기 (매일 시작할 때)
git switch -c feature/B-seat-map           # 브랜치 이름: feature/<담당>-<기능>

# ... AI와 작업, test.bat 통과 확인 ...

git status                                 # 내 폴더 밖 파일이 바뀌지 않았는지 꼭 확인
git add src/airline/ui/page/passenger test/airline/PassengerSeatTest.java docs/ai-log.md
git commit -m "[B] 좌석 배치도와 클릭 선택 추가 (AI: ChatGPT 3단계)"
git push -u origin feature/B-seat-map
```

GitHub에서 Pull Request를 만들고, 다른 한 명이 확인 후 Merge합니다.
리뷰하는 사람은 `test.bat` 통과와 "담당 폴더 밖 변경 없음"만 확인해도 충분합니다.

## 4. 커밋 메시지 규칙

```
[담당] 무엇을 했는지 (AI: 도구 단계)
```

예) `[A] 달력 컴포넌트 추가 (AI: Claude 3단계)` · `[C] 카드번호 4자리 띄어쓰기 수정 (AI 없음)` · `[공용] BookingSession에 메서드 추가 (요청 #12)`

작게 자주 커밋합니다. 하루에 한 번 이상 push하면 진행 이력이 자연스럽게 남습니다.

## 5. 공용 코드 변경

1. 담당자가 `docs/AI-GUIDE.md` 6번 요청서를 GitHub 이슈로 올림 (라벨 `공용변경`)
2. 통합 담당이 `feature/core-...` 브랜치에서 반영, `test.bat` 통과 확인 후 PR
3. 합쳐지면 단톡방에 알림 → 모두 `git switch main && git pull` 후 자기 브랜치에 `git merge main`

## 6. 충돌이 났을 때

자기 폴더만 고쳤다면 충돌은 주로 `docs/ai-log.md`에서 납니다. 두 사람의 줄을 모두 남기고 저장하면 됩니다.

```bash
git switch feature/B-seat-map
git merge main            # 충돌 파일 표시됨 → 파일을 열어 <<<< ==== >>>> 부분 정리
git add docs/ai-log.md
git commit
```

## 7. 올리면 안 되는 것

- `out/` (컴파일 결과), `context-pack-*.md` (AI 자료 묶음) → `.gitignore`에 이미 들어 있음
- 실제 개인정보, 실제 카드·계좌번호
- 다른 사람 폴더의 변경
