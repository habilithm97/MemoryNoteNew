# [기억노트 - 심플한 메모장]
## 딱 필요한 기능만 담은 가볍고 직관적인 메모장 앱
👉 Google Play : https://play.google.com/store/apps/details?id=com.habilithm.memorynote&pcampaignid=web_share

(휴지통, 로그인, 백업 및 불러오기 등 추후에 업데이트 예정)

<img src="https://github.com/user-attachments/assets/7ffad1e4-3c2e-413a-9fbb-292b78241c21" alt="MemoryNote 스크린샷 1" width="150"/>
<img src="https://github.com/user-attachments/assets/67bee1fd-1158-43e5-9118-e9ca20d4c681" alt="MemoryNote 스크린샷 4" width="150"/>
<img src="https://github.com/user-attachments/assets/372c4c21-3e6d-410e-a87d-68ab524a04b0" alt="MemoryNote 스크린샷 2" width="150"/>
<img src="https://github.com/user-attachments/assets/599bd553-5a6d-408f-9a38-cc6791c5ba77" alt="MemoryNote 스크린샷 3" width="150"/>

## 🛠️ 기술 스택
**Language**
- Kotlin

**Architecture**
- MVVM
- Repository Pattern
  
**Android Jetpack**
- Fragment
- RecyclerView
- ViewModel
- LiveData
- Lifecycle
- Room
- Preference

**UI**
- XML
- View Binding

**Backend / BaaS**
- Firebase Authentication
- Cloud Firestore

**Asynchronous**
- Kotlin Coroutines

**Security**
- Android Keystore System
- AES-GCM Encryption

## ✅ 주요 기능 및 핵심 구현 (Code Snippet)
- 메모 CRUD (Room Database)
- 메모 잠금 및 비밀번호 인증
- 메모 검색 (SearchView)
- 휴지통 (복원 및 30일 이후 자동 삭제)
- Google 로그인 (Firebase Authentication)
- 메모 백업 및 복원 (Cloud Firestore)
- 메모 내용 암호화 (AES/GCM Encryption)

### MVVM Architecture
UI와 데이터 계층을 분리
Fragment(UI) -> ViewModel -> Repository -> Room(DAO)
```kotlin
// ViewModel - UI와 데이터를 연결
class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val memoRepository = MemoRepository(
        MemoDatabase.getInstance(application).memoDao(),
        MemoDatabase.getInstance(application).trashDao()
    )
    // Room Flow → LiveData 변환 (UI에서 관찰)
    val getAllMemos: LiveData<List<Memo>> = memoRepository.getAllMemos().asLiveData()

    // Coroutine(viewModelScope)으로 비동기 DB 작업 처리
    fun insertMemo(memo: Memo) = viewModelScope.launch(Dispatchers.IO) {
        memoRepository.insertMemo(memo)
    }
}
```
### Firestore Backup
Room Database에 저장된 메모를 Firebase Authentication 사용자 UID 기준으로 Firestore에 백업
```kotlin
// Firestore에 메모 백업
suspend fun backup(memos: List<Memo>) {
    val uid = auth.currentUser?.uid ?: return

    val memoCollection = db.collection("users")
        .document(uid)
        .collection("memo")

    val batch = db.batch() // 여러 작업을 한 번에 처리하는 batch

    memos.forEach {
        batch.set(memoCollection.document(it.id.toString()), it)
    }
    batch.commit().await() // 비동기 commit
}
```
### Memo Encryption
Android Keystore 기반 AES 키를 사용해 메모 내용을 AES/GCM 방식으로 암호화
```kotlin
// Android Keystore에서 AES 키 생성 및 가져오기
private fun getOrCreateKey(): SecretKey {
    val keystore = KeyStore.getInstance("AndroidKeyStore").apply {
        load(null)
    }
    keystore.getKey("memo_aes_key", null)?.let {
        return it as SecretKey
    }
    val generator = KeyGenerator.getInstance(
        KeyProperties.KEY_ALGORITHM_AES,
        "AndroidKeyStore"
    )
    val spec = KeyGenParameterSpec.Builder(
        "memo_aes_key", KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
        .setRandomizedEncryptionRequired(true)
        .build()
    generator.init(spec)
    return generator.generateKey()
}

// 메모 내용 암호화
fun encrypt(plainText: String): Pair<ByteArray, ByteArray> {
    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    val secretKey = getOrCreateKey()

    cipher.init(Cipher.ENCRYPT_MODE, secretKey)
    val iv = cipher.iv
    val cipherText = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
    return Pair(cipherText, iv)
}
```
## 📂 프로젝트 구조
```text
app
 ┣ adapter
 ┃ ┣ MemoAdapter.kt
 ┃ ┗ TrashAdapter.kt
 ┣ common
 ┃ ┣ Constants.kt
 ┃ ┣ LockPasswordPurpose.kt
 ┃ ┣ LockPasswordState.kt
 ┃ ┣ LockPasswordString.kt
 ┃ ┗ PopupAction.kt
 ┣ repository
 ┃ ┣ FirestoreRepository.kt
 ┃ ┗ MemoRepository.kt
 ┣ room
 ┃ ┣ dao
 ┃ ┃ ┣ MemoDao.kt
 ┃ ┃ ┗ TrashDao.kt
 ┃ ┣ database
 ┃ ┃ ┗ MemoDatabase.kt
 ┃ ┗ entity
 ┃   ┣ Memo.kt
 ┃   ┗ Trash.kt
 ┣ security
 ┃ ┣ EncryptionManager.kt
 ┃ ┗ LockPasswordManager.kt
 ┣ ui
 ┃ ┣ activity
 ┃ ┃ ┣ MainActivity.kt
 ┃ ┃ ┗ SettingsActivity.kt
 ┃ ┗ fragment
 ┃   ┣ DeleteAccountFragment.kt
 ┃   ┣ ForgotPasswordFragment.kt
 ┃   ┣ ListFragment.kt
 ┃   ┣ LockPasswordFragment.kt
 ┃   ┣ MemoFragment.kt
 ┃   ┣ SettingsFragment.kt
 ┃   ┣ SignInFragment.kt
 ┃   ┗ TrashFragment.kt
 ┣ utils
 ┃ ┣ ToastUtil.kt
 ┃ ┗ VibrateUtil.kt
 ┗ viewmodel
   ┗ MainViewModel.kt
```
## 출처
폰트 : https://noonnu.cc/font_page/1084
