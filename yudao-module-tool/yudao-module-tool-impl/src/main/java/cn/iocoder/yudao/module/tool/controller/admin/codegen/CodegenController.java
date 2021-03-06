package cn.iocoder.yudao.module.tool.controller.admin.codegen;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.ZipUtil;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.tool.controller.admin.codegen.vo.CodegenDetailRespVO;
import cn.iocoder.yudao.module.tool.controller.admin.codegen.vo.CodegenPreviewRespVO;
import cn.iocoder.yudao.module.tool.controller.admin.codegen.vo.CodegenUpdateReqVO;
import cn.iocoder.yudao.module.tool.controller.admin.codegen.vo.table.CodegenTablePageReqVO;
import cn.iocoder.yudao.module.tool.controller.admin.codegen.vo.table.CodegenTableRespVO;
import cn.iocoder.yudao.module.tool.controller.admin.codegen.vo.table.SchemaTableRespVO;
import cn.iocoder.yudao.module.tool.convert.codegen.CodegenConvert;
import cn.iocoder.yudao.module.tool.dal.dataobject.codegen.CodegenColumnDO;
import cn.iocoder.yudao.module.tool.dal.dataobject.codegen.CodegenTableDO;
import cn.iocoder.yudao.module.tool.dal.dataobject.codegen.SchemaTableDO;
import cn.iocoder.yudao.module.tool.service.codegen.CodegenService;
import cn.iocoder.yudao.framework.common.util.collection.CollectionUtils;
import cn.iocoder.yudao.framework.common.util.servlet.ServletUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Api(tags = "???????????? - ???????????????")
@RestController
@RequestMapping("/tool/codegen")
@Validated
public class CodegenController {

    @Resource
    private CodegenService codegenService;

    @GetMapping("/db/table/list")
    @ApiOperation(value = "???????????????????????????????????????", notes = "???????????????????????? Codegen ??????")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "tableName", value = "?????????????????????", required = true, example = "yudao", dataTypeClass = String.class),
            @ApiImplicitParam(name = "tableComment", value = "?????????????????????", required = true, example = "??????", dataTypeClass = String.class)
    })
    @PreAuthorize("@ss.hasPermission('tool:codegen:query')")
    public CommonResult<List<SchemaTableRespVO>> getSchemaTableList(
            @RequestParam(value = "tableName", required = false) String tableName,
            @RequestParam(value = "tableComment", required = false) String tableComment) {
        // ???????????????????????????????????????
        List<SchemaTableDO> schemaTables = codegenService.getSchemaTableList(tableName, tableComment);
        // ????????? Codegen ?????????????????????
        Set<String> existsTables = CollectionUtils.convertSet(codegenService.getCodeGenTableList(), CodegenTableDO::getTableName);
        schemaTables.removeIf(table -> existsTables.contains(table.getTableName()));
        return success(CodegenConvert.INSTANCE.convertList04(schemaTables));
    }

    @GetMapping("/table/page")
    @ApiOperation("?????????????????????")
    @PreAuthorize("@ss.hasPermission('tool:codegen:query')")
    public CommonResult<PageResult<CodegenTableRespVO>> getCodeGenTablePage(@Valid CodegenTablePageReqVO pageReqVO) {
        PageResult<CodegenTableDO> pageResult = codegenService.getCodegenTablePage(pageReqVO);
        return success(CodegenConvert.INSTANCE.convertPage(pageResult));
    }

    @GetMapping("/detail")
    @ApiOperation("???????????????????????????")
    @ApiImplicitParam(name = "tableId", value = "?????????", required = true, example = "1024", dataTypeClass = Long.class)
    @PreAuthorize("@ss.hasPermission('tool:codegen:query')")
    public CommonResult<CodegenDetailRespVO> getCodegenDetail(@RequestParam("tableId") Long tableId) {
        CodegenTableDO table = codegenService.getCodegenTablePage(tableId);
        List<CodegenColumnDO> columns = codegenService.getCodegenColumnListByTableId(tableId);
        // ????????????
        return success(CodegenConvert.INSTANCE.convert(table, columns));
    }

    @ApiOperation("????????????????????????????????????????????????????????????????????????")
    @ApiImplicitParam(name = "tableNames", value = "????????????", required = true, example = "sys_user", dataTypeClass = List.class)
    @PostMapping("/create-list-from-db")
    @PreAuthorize("@ss.hasPermission('tool:codegen:create')")
    public CommonResult<List<Long>> createCodegenListFromDB(@RequestParam("tableNames") List<String> tableNames) {
        return success(codegenService.createCodegenListFromDB(getLoginUserId(), tableNames));
    }

    @ApiOperation("?????? SQL ?????????????????????????????????????????????????????????")
    @ApiImplicitParam(name = "sql", value = "SQL ????????????", required = true, example = "sql", dataTypeClass = String.class)
    @PostMapping("/create-list-from-sql")
    @PreAuthorize("@ss.hasPermission('tool:codegen:create')")
    public CommonResult<Long> createCodegenListFromSQL(@RequestParam("sql") String sql) {
        return success(codegenService.createCodegenListFromSQL(getLoginUserId(), sql));
    }

    @ApiOperation("????????????????????????????????????")
    @PutMapping("/update")
    @PreAuthorize("@ss.hasPermission('tool:codegen:update')")
    public CommonResult<Boolean> updateCodegen(@Valid @RequestBody CodegenUpdateReqVO updateReqVO) {
        codegenService.updateCodegen(updateReqVO);
        return success(true);
    }

    @ApiOperation("??????????????????????????????????????????????????????????????????")
    @PutMapping("/sync-from-db")
    @ApiImplicitParam(name = "tableId", value = "?????????", required = true, example = "1024", dataTypeClass = Long.class)
    @PreAuthorize("@ss.hasPermission('tool:codegen:update')")
    public CommonResult<Boolean> syncCodegenFromDB(@RequestParam("tableId") Long tableId) {
        codegenService.syncCodegenFromDB(tableId);
        return success(true);
    }

    @ApiOperation("?????? SQL ???????????????????????????????????????????????????")
    @PutMapping("/sync-from-sql")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "tableId", value = "?????????", required = true, example = "1024", dataTypeClass = Long.class),
            @ApiImplicitParam(name = "sql", value = "SQL ????????????", required = true, example = "sql", dataTypeClass = String.class)
    })
    @PreAuthorize("@ss.hasPermission('tool:codegen:update')")
    public CommonResult<Boolean> syncCodegenFromSQL(@RequestParam("tableId") Long tableId,
                                                    @RequestParam("sql") String sql) {
        codegenService.syncCodegenFromSQL(tableId, sql);
        return success(true);
    }

    @ApiOperation("????????????????????????????????????")
    @DeleteMapping("/delete")
    @ApiImplicitParam(name = "tableId", value = "?????????", required = true, example = "1024", dataTypeClass = Long.class)
    @PreAuthorize("@ss.hasPermission('tool:codegen:delete')")
    public CommonResult<Boolean> deleteCodegen(@RequestParam("tableId") Long tableId) {
        codegenService.deleteCodegen(tableId);
        return success(true);
    }

    @ApiOperation("??????????????????")
    @GetMapping("/preview")
    @ApiImplicitParam(name = "tableId", value = "?????????", required = true, example = "1024", dataTypeClass = Long.class)
    @PreAuthorize("@ss.hasPermission('tool:codegen:preview')")
    public CommonResult<List<CodegenPreviewRespVO>> previewCodegen(@RequestParam("tableId") Long tableId) {
        Map<String, String> codes = codegenService.generationCodes(tableId);
        return success(CodegenConvert.INSTANCE.convert(codes));
    }

    @ApiOperation("??????????????????")
    @GetMapping("/download")
    @ApiImplicitParam(name = "tableId", value = "?????????", required = true, example = "1024", dataTypeClass = Long.class)
    @PreAuthorize("@ss.hasPermission('tool:codegen:download')")
    public void downloadCodegen(@RequestParam("tableId") Long tableId,
                                HttpServletResponse response) throws IOException {
        // ????????????
        Map<String, String> codes = codegenService.generationCodes(tableId);
        // ?????? zip ???
        String[] paths = codes.keySet().toArray(new String[0]);
        ByteArrayInputStream[] ins = codes.values().stream().map(IoUtil::toUtf8Stream).toArray(ByteArrayInputStream[]::new);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ZipUtil.zip(outputStream, paths, ins);
        // ??????
        ServletUtils.writeAttachment(response, "codegen.zip", outputStream.toByteArray());
    }

}
