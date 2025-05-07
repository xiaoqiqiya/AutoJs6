@Override
protected void onNewIntent(Intent intent) {
    super.onNewIntent(intent);
    Uri uri = intent.getData();
    if (uri != null && "autojs6".equals(uri.getScheme())) {
        // 处理scheme跳转，唤醒app
        setIntent(intent);
    }
} 