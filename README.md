# [기억노트 - 심플한 메모장]
## 딱 필요한 기능만 담은 가볍고 직관적인 메모장 앱
#### 👉 Google Play : https://play.google.com/store/apps/details?id=com.habilithm.memorynote&pcampaignid=web_share
<img src="https://github.com/user-attachments/assets/7ffad1e4-3c2e-413a-9fbb-292b78241c21" alt="MemoryNote 스크린샷 1" width="150"/>
<img src="https://github.com/user-attachments/assets/67bee1fd-1158-43e5-9118-e9ca20d4c681" alt="MemoryNote 스크린샷 4" width="150"/>
<img src="https://github.com/user-attachments/assets/372c4c21-3e6d-410e-a87d-68ab524a04b0" alt="MemoryNote 스크린샷 2" width="150"/>
<img src="https://github.com/user-attachments/assets/599bd553-5a6d-408f-9a38-cc6791c5ba77" alt="MemoryNote 스크린샷 3" width="150"/>

## ✅ 주요 기능
### 📝 메모 작성 및 관리
```kotlin
// DB 초기화를 위한 Application Context 사용
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val memoRepository = MemoRepository(MemoDatabase.getInstance(application).memoDao())

    // Flow → LiveData (UI 관찰)
    val getAllMemos: LiveData<List<Memo>> = memoRepository.getAllMemos().asLiveData()

    // viewModelScope에서 IO 스레드로 suspend 작업 실행
    private fun <T> launchIO(block: suspend () -> T) {
        // 지금 실행하지 않고 나중에 IO 코루틴 안에서 실행할 코드 블록
        viewModelScope.launch(Dispatchers.IO) { block() }
    }
    fun insertMemo(memo: Memo) = launchIO {
        memoRepository.insertMemo(memo)
    }
    fun updateMemo(memo: Memo) = launchIO {
        memoRepository.updateMemo(memo)
    }
    fun deleteMemo(memo: Memo) = launchIO {
        memoRepository.deleteMemo(memo)
    }
}
```
- 메모 작성, 수정, 삭제 기능 제공
- Room Database를 통한 로컬 메모 저장
- ViewModel로 UI와 데이터 로직 분리
### 🔒 메모 잠금
- 잠긴 메모는 잠금 비밀번호 입력 후 접근 가능
- 잠금 비밀번호 상태를 enum으로 관리하여 흐름 명확화
### 🔍 메모 검색
```kotlin
// Fragment -> Adapter로 검색어 전달
private fun setupSearchView() {
    binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
        // 검색어 입력 시 호출
        override fun onQueryTextChange(newText: String?): Boolean {
            memoAdapter.filterList(newText.orEmpty()) // null이면 "" 처리
            return true
        }
        override fun onQueryTextSubmit(query: String?): Boolean = false
    })
}
```
- SearchView 기반 메모 검색 기능
- UI에서는 검색어 전달만 담당 (필터 로직 분리)
```kotlin
// Adapter 필터링 로직

private var memos: List<Memo> = emptyList() // 원본 리스트 유지

fun submitMemos(memos: List<Memo>) {
    this.memos = memos // 원본 리스트 보관
    submitList(memos)
}

fun filterList(query: String, onFilterComplete: () -> Unit) {
        val filteredList = if (query.isEmpty()) {
            memos
        } else { // 공백도 검색 가능
            memos.filter {
                it.content.contains(query, true) // 대소문자 구분 없이 검색
            }
        }
        submitList(filteredList) {
            onFilterComplete() // 필터링 후속 작업 (자동 스크롤)
        }
    }
```
- Adapter 필터링으로 빠른 목록 갱신
### 🗑️ 휴지통
- 삭제된 메모를 휴지통으로 이동
- 복원 및 완전 삭제 기능 제공
- Room의 별도 Entity로 데이터 안정성 확보
### 🔐 로그인 / 백업 및 불러오기
- Firebase Authentication 기반 Google 로그인 기능
- 인증된 사용자만 메모를 서버에 안전하게 보관 가능

## 📂 프로젝트 구조 (com.example.memorynotenew)
- **adapter**
  - MemoAdapter
  - TrashAdapter
- **common**
  - Constants
  - LockPasswordPurpose
  - LockPasswordState
  - LockPasswordString
  - PopupAction
- **repository**
  - FirestoreRepository
  - MemoRepository
- **room**
  - **dao**
    - MemoDao
    - TrashDao
  - **database**
    - MemoDatabase
  - **entity**
    - Memo
    - Trash
- **security**
  - EncryptionManager
  - LockPasswordManager
- **ui**
  - **activity**
    - MainActivity
    - SettingsActivity
  - **fragment**
    - DeleteAccountFragment
    - ForgotPasswordFragment
    - ListFragment
    - LockPasswordFragment
    - MemoFragment
    - SettingsFragment
    - SignInFragment
    - TrashFragment
- **utils**
  - ToastUtil
  - VibrateUtil
- **viewmodel**
  - MainViewModel

## Credits
Font : https://noonnu.cc/font_page/1084
