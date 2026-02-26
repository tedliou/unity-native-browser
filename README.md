開發一個給 Unity 使用的 Android 原生網頁瀏覽功能插件 .aar

基本組成：
- src/android: aar 專案
- src/unity: unity 專案
- .agents/: AI閱讀的規範等資源文件
- docs: 給開發者看的文件，需有英文與繁體中文(以github的慣例寫法進行)
- tools/: 自動化腳本，包括觸發測試、編譯、建置、複製、清除，該設計未來要可提供CI使用

流程結構:
- unity 呼叫作為SDK的package中的API
- API啟動android對應網頁元件，覆蓋在unity的遊戲上

需要的功能:
- 開啟指定網頁
- 關閉網頁
- 重新整理
- 接收postmessage
- 執行js
- 注入js
- 設定寬高、單純設定寬、設定畫面對齊(例如只設定寬的比例0.75，但對齊左側，webview會靠左對齊，寬約3/4)
- 若webview沒填滿，當點擊空白區域(unity的畫面)，網頁會關閉
- 可供設定deeplink，當網頁被導向到deeplink時，會自動關閉網頁或執行某作業
- 要有完整的單元測試 (覆蓋率85%以上)
- 要有完整的整合測試，須建立mock web page，來實際跑 edit mode 或 play mode 測試
- 能啟動的網頁瀏覽元件包含webview(功能都要實作)、custom tabs (部分功能)、系統瀏覽器 (主要是能跳轉，我理解許多功能無法實現)

驗證：
- 模擬多種目前常見的網頁設計與行為，確保功能正常
- 需要模擬當網頁載入完成時，網頁會發出postmessage訊號，當unity收到時會執行一段Js，來驅動網頁互動
- 需要模擬當unity執行js時，網頁能回傳數值，unity可收到

規範：
- 保持程式碼有清晰、專業、設計模式的架構
- 要有良好的log規劃
- 要有良好的英文註解
- 要有良好的git歷程紀錄
- agent如果只有要讀檔，不要讓subagent用回傳整個檔案內容的方式進行，建議回傳路徑與行數，讓parent直接讀
- 若作業能寫程式快速完成，就建議寫腳本，減少token與時間的浪費
- 要注重資訊安全
- 不可擅自對此專案目錄之外的檔案進行高風險操作
- 功能設計完成時，需撰寫完整docs文件，此readme.md可備份後複寫
- 無用程式碼與package要移除
- 若有新版本package建議升級
- 變更規範或架構時，須同步於.agents或AGENTS.md等相關文件進行更新，以確保agent順利運作
- 變更架構或功能時，須同步於docs上進行更新，以確保文件最新
- 非git需收錄的檔案，不用收錄

備註：
- 請嘗試存取headless unity、android專案、android vm (已開好)
- 最終須於android vm上安裝好編譯後的apk並執行，讓我看
- AI與使用者對話需用繁體中文，其他時間用英文節省token
