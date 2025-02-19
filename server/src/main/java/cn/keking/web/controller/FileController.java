package cn.keking.web.controller;

import cn.keking.config.ConfigConstants;
import cn.keking.model.ReturnResponse;
import cn.keking.utils.KkFileUtils;
import cn.keking.utils.WebUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author yudian-it
 *  2017/12/1
 */
@RestController
public class FileController {

    private final Logger logger = LoggerFactory.getLogger(FileController.class);

    private final String fileDir = ConfigConstants.getFileDir();
    private final String demoDir = "demo";
    private final String demoPath = demoDir + File.separator;
    public static final String BASE64_DECODE_ERROR_MSG = "Base64解码失败，请检查你的 %s 是否采用 Base64 + urlEncode 双重编码了！";
    private static final String[] not_allowed = { "dll", "exe", "msi" }; // 不允许上传的文件扩展名

    @PostMapping("/fileUpload")
    public ReturnResponse<Object> fileUpload(@RequestParam("file") MultipartFile file) {
        ReturnResponse<Object> checkResult = this.fileUploadCheck(file);
        if (checkResult.isFailure()) {
            return checkResult;
        }
        File outFile = new File(fileDir + demoPath);
        if (!outFile.exists() && !outFile.mkdirs()) {
            logger.error("创建文件夹【{}】失败，请检查目录权限！", fileDir + demoPath);
        }
        String fileName = checkResult.getContent().toString();
        logger.info("上传文件：{}{}{}", fileDir, demoPath, fileName);
        try (InputStream in = file.getInputStream(); OutputStream out = Files.newOutputStream(Paths.get(fileDir + demoPath + fileName))) {
            StreamUtils.copy(in, out);
            return ReturnResponse.success(null);
        } catch (IOException e) {
            logger.error("文件上传失败", e);
            return ReturnResponse.failure();
        }
    }

    @GetMapping("/deleteFile")
    public ReturnResponse<Object> deleteFile(String fileName) {
        ReturnResponse<Object> checkResult = this.deleteFileCheck(fileName);
        if (checkResult.isFailure()) {
            return checkResult;
        }
        fileName = checkResult.getContent().toString();
        File file = new File(fileDir + demoPath + fileName);
        logger.info("删除文件：{}", file.getAbsolutePath());
        if (file.exists() && !file.delete()) {
            String msg = String.format("删除文件【%s】失败，请检查目录权限！", file.getPath());
            logger.error(msg);
            return ReturnResponse.failure(msg);
        }
        return ReturnResponse.success();
    }

    @GetMapping("/listFiles")
    public List<Map<String, String>> getFiles() {
        List<Map<String, String>> list = new ArrayList<>();
        File file = new File(fileDir + demoPath);
        if (file.exists()) {
            File[] files = Objects.requireNonNull(file.listFiles());
            Arrays.sort(files, (f1, f2) -> Long.compare(f2.lastModified(), f1.lastModified()));
            Arrays.stream(files).forEach(file1 -> {
                Map<String, String> fileName = new HashMap<>();
                fileName.put("fileName", demoDir + "/" + file1.getName());
                list.add(fileName);
            });
        }
        return list;
    }

    /**
     * 上传文件前校验
     *
     * @param file 文件
     * @return 校验结果
     */
    private ReturnResponse<Object> fileUploadCheck(MultipartFile file) {
        if (ConfigConstants.getFileUploadDisable()) {
            return ReturnResponse.failure("文件传接口已禁用");
        }
        String fileName = WebUtils.getFileNameFromMultipartFile(file);

        if (!isAllowedUpload(fileName)) {
            return ReturnResponse.failure("不允许上传的文件类型: " + fileName);
        }
        if (KkFileUtils.isIllegalFileName(fileName)) {
            return ReturnResponse.failure("不允许上传的文件名: " + fileName);
        }
        // 判断是否存在同名文件
        if (existsFile(fileName)) {
            return ReturnResponse.failure("存在同名文件，请先删除原有文件再次上传");
        }
        return ReturnResponse.success(fileName);
    }

    /**
     * 判断文件是否允许上传
     *
     * @param file 文件扩展名
     * @return 是否允许上传
     */
    private boolean isAllowedUpload(String file) {
        String fileType = KkFileUtils.suffixFromFileName(file);
        for (String type : not_allowed) {
            if (type.equals(fileType))
                return false;
        }
        return !ObjectUtils.isEmpty(fileType);
    }

    /**
     * 删除文件前校验
     *
     * @param fileName 文件名
     * @return 校验结果
     */
    private ReturnResponse<Object> deleteFileCheck(String fileName) {
        if (ObjectUtils.isEmpty(fileName)) {
            return ReturnResponse.failure("文件名为空，删除失败！");
        }
        try {
            fileName = WebUtils.decodeUrl(fileName);
        } catch (Exception ex) {
            String errorMsg = String.format(BASE64_DECODE_ERROR_MSG, fileName);
            return ReturnResponse.failure(errorMsg + "删除失败！");
        }
        assert fileName != null;
        if (fileName.contains("/")) {
            fileName = fileName.substring(fileName.lastIndexOf("/") + 1);
        }
        if (KkFileUtils.isIllegalFileName(fileName)) {
            return ReturnResponse.failure("非法文件名，删除失败！");
        }
        return ReturnResponse.success(fileName);
    }

    private boolean existsFile(String fileName) {
        File file = new File(fileDir + demoPath + fileName);
        return file.exists();
    }
}
