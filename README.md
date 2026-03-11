# [기억노트 - 심플한 메모장]
## 딱 필요한 기능만 담은 가볍고 직관적인 메모장 앱
#### 👉 Google Play : https://play.google.com/store/apps/details?id=com.habilithm.memorynote&pcampaignid=web_share

(휴지통, 로그인, 백업 및 불러오기 등 추후에 업데이트 예정)

<img src="https://github.com/user-attachments/assets/7ffad1e4-3c2e-413a-9fbb-292b78241c21" alt="MemoryNote 스크린샷 1" width="150"/>
<img src="https://github.com/user-attachments/assets/67bee1fd-1158-43e5-9118-e9ca20d4c681" alt="MemoryNote 스크린샷 4" width="150"/>
<img src="https://github.com/user-attachments/assets/372c4c21-3e6d-410e-a87d-68ab524a04b0" alt="MemoryNote 스크린샷 2" width="150"/>
<img src="https://github.com/user-attachments/assets/599bd553-5a6d-408f-9a38-cc6791c5ba77" alt="MemoryNote 스크린샷 3" width="150"/>

## 🛠️ 기술 스택
**Language**
- Kotlin
- XML (Extensible Markup Language)

**Architecture**
- MVVM (Model–View–ViewModel)
- Repository Pattern
  
**Android Jetpack**
- ViewModel
- LiveData
- Lifecycle
- Room Database
- Fragment
- RecyclerView
- View Binding
- Preference

**Asynchronous Programming**
- Kotlin Coroutines

**Backend / BaaS**
- Firebase Authentication
- Cloud Firestore

**Security**
- Android Keystore System
- AES (Advanced Encryption Standard) / GCM (Galois/Counter Mode)
- Data Encryption

## ✅ 주요 기능 (Snippet)
### 📝 메모 작성 및 관리
- 메모 작성, 수정, 삭제 기능 제공 (CRUD)
- Room Database를 통한 로컬 메모 저장
- MVVM 패턴을 적용하여 ViewModel에서 데이터를 관리하고, UI는 변경 사항만 관찰하도록 구성
```kotlin
// DB 초기화를 위한 Application Context 사용
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val memoRepository = MemoRepository(MemoDatabase.getInstance(application).memoDao())

    // Flow → LiveData (UI 관찰)
    val getAllMemos: LiveData<List<Memo>> = memoRepository.getAllMemos().asLiveData()

    // IO 스레드에서 DB 작업을 실행하기 위한 공통 메서드
    private fun <T> launchIO(block: suspend () -> T) {
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
<br>

### 🔐 메모 잠금 및 잠금 해제
- 메모 잠금 및 잠금 해제 기능 제공
- 잠긴 메모는 비밀번호 인증 후 열기 및 삭제 가능
- 잠금 상태에 따른 화면 이동 로직 분기 처리
```kotlin
ListFragment.kt

private fun setupAdapter() {
    memoAdapter = MemoAdapter(
        // 메모 클릭
        onItemClick = { memo ->
            val targetFragment = if (memo.isLocked) { // 잠긴 메모 → 비밀번호 인증 후 열기
                PasswordFragment.newInstance(PasswordPurpose.OPEN, memo)
            } else { // 잠기지 않은 메모 → 바로 열기
                MemoFragment().apply {
                    arguments = Bundle().apply {
                        putParcelable(MEMO, memo)
                    }
                }
            }
            replaceFragment(targetFragment)
        },
        // 메모 롱클릭 시 팝업 메뉴 동작 처리 (삭제 및 잠금)
        onItemLongClick = { memo, popupAction ->
            when (popupAction) {
                PopupAction.DELETE -> { // 삭제 동작
                    if (memo.isLocked) { // 잠긴 메모 -> 비밀번호 인증 후 삭제
                        replaceFragment(PasswordFragment.newInstance(PasswordPurpose.DELETE, memo))
                    } else { // 잠기지 않은 메모 -> 바로 삭제
                        mainViewModel.moveMemoToTrash(memo)
                    }
                }
                PopupAction.LOCK -> { // 잠금 및 잠금 해제 동작
                    val storedPassword = PasswordManager.getPassword(requireContext())

                    if (storedPassword.isNullOrEmpty()) { // 비밀번호 존재X -> 안내 메시지 표시
                        requireContext().showToast(getString(R.string.set_password_first))
                    } else { // 비밀번호 존재 -> 비밀번호 인증 후 처리
                        replaceFragment(PasswordFragment.newInstance(PasswordPurpose.LOCK, memo))
                    }
                }
            }
        }
    )
}
```
<br>

### 🔍 메모 검색
- SearchView 기반 메모 검색 기능 제공
- UI에서는 검색어 전달만 담당 (필터 로직 분리)
```kotlin
ListFragment.kt

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
<br>

- Adapter 필터링으로 빠른 목록 갱신
```kotlin
MemoAdapter.kt

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
<br>

### 🗑️ 휴지통
- 별도의 Room Entity로 데이터 안정성 확보 가능

  -> 원본 데이터와 분리, 삭제/복원 과정에서 데이터 안정성 확보
- Fragment 간 데이터 전달을 위해 Parcelable 사용
```kotlin
// DB 테이블에 매핑되는 Entity
@Parcelize // Parcelable 보일러플레이트 제거
@Entity
data class Trash(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val content: String,
    val deletedAt: Long // 삭제 시점
) : Parcelable
```
<br>

- 삭제된 메모를 휴지통으로 이동
- 삭제 시 원본 메모와 분리된 Trash Entity로 이동
```kotlin
MainViewModel.kt

fun moveMemoToTrash(memo: Memo) = launchIO {
        val trash = Trash(
            content = memo.content,
            deletedAt = System.currentTimeMillis()
        )
        with(memoRepository) {
            insertTrash(trash) // 휴지통에 추가
            deleteMemo(memo) // 메모 삭제
        }
}
```
<br>

- 휴지통에서 메모 복원
```kotlin
MainViewModel.kt

fun restoreMemo(trash: Trash) = launchIO {
        val memo = Memo(
            id = 0,
            content = trash.content,
            date = System.currentTimeMillis(),
            isLocked = false
        )
        with(memoRepository) {
            insertMemo(memo) // 메모 추가
            deleteTrash(trash) // 휴지통에서 삭제
        }
}
```
- 휴지통에서는 개별 메모 삭제, 전체 비우기, 30일 이후 자동 삭제 기능을 제공하여 저장 공간을 효율적으로 관리
<br>

### 🔐 로그인 / 인증
- Firebase Authentication 기반 Google 로그인 기능 제공
- 사용자는 구글 계정으로 로그인
```kotlin
SignInFragment.kt

private fun firebaseAuthWithGoogle(idToken: String) {
        // Google ID Token으로 Firebase 인증 Credential 생성
        val credential = GoogleAuthProvider.getCredential(idToken, null)

        // Firebase 인증 시도
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) { // 로그인 성공
                    // 인증 완료 후 이전 화면으로 복귀
                    requireActivity().supportFragmentManager.popBackStack()
                } else { // 로그인 실패
                    Log.e("SignInFragment", "Firebase 인증 실패", task.exception)
                }
            }
}
```
<br>

### ↕️ 메모 백업 및 불러오기
- 로그인한 사용자 UID별로 Firestore에 메모 백업 기능 제공
- Batch를 사용하여 여러 메모를 한 번에 처리
- Firestore 구조 : /users/{uid}/memo/{memoId}
```kotlin
FirestoreRepository.kt

suspend fun backup(memos: List<Memo>) {
        val uid = auth.currentUser?.uid ?: return

        val memoCollection = db.collection(USERS)
            .document(uid)
            .collection(MEMO)

        val batch = db.batch()

        memos.forEach {
            batch.set(memoCollection.document(it.id.toString()), it)
        }
        batch.commit().await()
}
```
<br>

- 서버 데이터를 로컬 DB로 복원 기능 제공
- 복원 시 로컬 DB에서 autoGenerate 되도록 id를 0으로 초기화
```kotlin
FirestoreRepository.kt

suspend fun load(): List<Memo> {
        val uid = auth.currentUser?.uid ?: return emptyList()

        val snapshot = db.collection(USERS)
            .document(uid)
            .collection(MEMO)
            .get().await()

        return snapshot.documents.mapNotNull {
            val content = it.getString(CONTENT) ?: return@mapNotNull null
            val date = it.getLong(DATE) ?: return@mapNotNull null
            val isLocked = it.getBoolean(IS_LOCKED) ?: false
            val iv = it.getString(IV) ?: return@mapNotNull null

            Memo(
                id = 0,
                content = content,
                date = date,
                isLocked = isLocked,
                iv = iv
            )
        }
}
```
<br>

### 🔐 AES/GCM 문자열 암호화 및 복호화
- Android Keystore 안에서 AES 키를 생성 및 관리
- 메모별 랜덤 IV로 동일한 내용도 매번 다른 암호문 생성
- GCM 모드 사용 -> 데이터 무결성 (변조 검출) 보
```kotlin
EncryptionManager.kt

// 암호화
fun encrypt(plainText: String): Pair<ByteArray, ByteArray> {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val secretKey = getOrCreateKey()
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val iv = cipher.iv // 랜덤 IV (복호화 시 필요)
        val cipherText = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        return Pair(cipherText, iv)
}
// 복호화
fun decrypt(cipherText: ByteArray, iv: ByteArray): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val secretKey = getOrCreateKey()
        val spec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
        val plainBytes = cipher.doFinal(cipherText)
        return String(plainBytes, Charsets.UTF_8)
}
```

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

## 출처
폰트 : https://noonnu.cc/font_page/1084
