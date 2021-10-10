# Android Local Backup Task

## Backup

```java
BackupTask.scheduleBackup(context);
```

## Restore

```java
new Thread(() -> {
    if (BackupTask.isDataFound()) {
        BackupTask.restore(context);
        BackupTask.clear();
        runOnUiThread(() -> {
            Toast.makeText(this, "Data restored.", Toast.LENGTH_LONG).show();
        });
    }
}).start();
```

