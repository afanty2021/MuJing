# Whisper Medium 模型下载指南

## 📥 下载信息

- **文件名**: ggml-medium.bin
- **大小**: 1.5 GB (1,534,236,619 字节)
- **用途**: 中文语音识别（支持 99 种语言）
- **MD5 校验**: 可选验证

## 🔗 下载链接

### 国内镜像（推荐 - 速度快）
```
https://hf-mirror.com/ggerganov/whisper.cpp/resolve/main/ggml-medium.bin
```

### 官方源
```
https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-medium.bin
```

## 📁 保存位置

下载完成后，将文件保存到：
```
D:/Berton/MuJing/src/test/resources/whisper/ggml-medium.bin
```

## ⬇️ 下载方法

### 方法 1: 浏览器下载（最简单）
1. 复制下载链接
2. 在浏览器中打开
3. 等待下载完成（约 10-30 分钟）
4. 将文件移动到指定位置

### 方法 2: 使用 IDM/迅雷（推荐）
1. 打开下载管理器
2. 新建任务，粘贴下载链接
3. 选择保存位置：`D:/Berton/MuJing/src/test/resources/whisper/`
4. 开始下载（支持断点续传）

### 方法 3: 命令行下载
```bash
# 使用 wget
wget -c https://hf-mirror.com/ggerganov/whisper.cpp/resolve/main/ggml-medium.bin
mv ggml-medium.bin "D:/Berton/MuJing/src/test/resources/whisper/"

# 使用 aria2（多线程，速度最快）
aria2c -x 16 -s 16 https://hf-mirror.com/ggerganov/whisper.cpp/resolve/main/ggml-medium.bin
mv ggml-medium.bin "D:/Berton/MuJing/src/test/resources/whisper/"
```

## ✅ 验证下载

下载完成后，运行以下命令验证：

```bash
ls -lh "D:/Berton/MuJing/src/test/resources/whisper/ggml-medium.bin"
```

预期输出：
```
-rw-r--r-- 1 Berton 197121 1.5G [日期时间] ggml-medium.bin
```

## 🔄 下载完成后

下载完成后，告诉 AI："下载完成了"，系统会自动：
1. 验证文件完整性
2. 创建中文语音识别测试
3. 对比中英文识别效果

## 📝 注意事项

- **文件较大**: 确保有足够的磁盘空间（至少 3GB 可用空间）
- **网络稳定**: 建议使用稳定的网络连接
- **断点续传**: 推荐使用支持断点续传的下载工具
- **文件完整性**: 下载后检查文件大小是否接近 1.5GB

---

**下载链接**: https://hf-mirror.com/ggerganov/whisper.cpp/resolve/main/ggml-medium.bin

**保存位置**: `D:/Berton/MuJing/src/test/resources/whisper/ggml-medium.bin`
