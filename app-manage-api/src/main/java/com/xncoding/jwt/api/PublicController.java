package com.xncoding.jwt.api;

import com.xncoding.jwt.api.model.*;
import com.xncoding.jwt.common.dao.entity.Pos;
import com.xncoding.jwt.common.dao.entity.Project;
import com.xncoding.jwt.service.ApiService;
import com.xncoding.jwt.service.ManagerInfoService;
import io.swagger.annotations.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.authz.annotation.RequiresUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.Date;

/**
 * 机具管理API接口类
 */
@Api(value = "机具管理业务接口类", tags = "机具管理", description = "机具管理业务接口类")
@ApiResponses(value = {
        @ApiResponse(code = 200, message = "请求已完成"),
        @ApiResponse(code = 201, message = "资源成功被创建"),
        @ApiResponse(code = 400, message = "请求中有语法问题，或不能满足请求"),
        @ApiResponse(code = 401, message = "未授权客户机访问数据"),
        @ApiResponse(code = 403, message = "服务器接受请求，但是拒绝处理"),
        @ApiResponse(code = 404, message = "服务器找不到给定的资源；文档不存在"),
        @ApiResponse(code = 500, message = "服务器出现异常")}
)
@RestController
@RequestMapping(value = "/api/v1")
public class PublicController {

    @Resource
    private ApiService apiService;

    @Resource
    private ManagerInfoService managerInfoService;

    private static final Logger _logger = LoggerFactory.getLogger(PublicController.class);

    /**
     * 入网绑定查询接口
     *
     * @return 是否入网
     */
    @ApiOperation(value = "入网绑定查询接口", notes = "根据IMEI码来查询该POS机是否入网并绑定了网点<br/>每次打开APP的时候需要调用这个接口来进行检查<br/>如果入网并且绑定了网点才能打开APP，否则必须调用入网或绑定网点接口成功后才能继续下一步", produces = "application/json")
    @ApiImplicitParam(name = "imei", value = "IMEI码", required = true, dataType = "String", paramType = "query", defaultValue = "1234555SHA")
    @RequestMapping(value = "/join", method = RequestMethod.GET)
    @RequiresUser
    public JoinBindResponse join(@RequestHeader(name="Accept",  defaultValue = "application/json", required = false) String accept,
                                 @RequestHeader(name="Authorization", defaultValue="token") String token,
                                 @RequestParam("imei") String imei) {
        _logger.info("入网查询接口 start....");
        JoinBindResponse result = new JoinBindResponse();
        int posCount = apiService.selectCount(imei);
        if (posCount > 0) {
            // 如果入网了再去查询是否绑定了网点
            int bindCount = apiService.selectBindCount(imei);
            Pos pos = apiService.selectByImei(imei);
            result.setPosState(pos.getPosState());
            if (bindCount == 0) {
                result.setSuccess(false);
                result.setCode(2);
                result.setMsg("已入网但尚未绑定网点");
            } else {
                result.setSuccess(true);
                result.setCode(1);
                result.setMsg("已入网并绑定了网点");
            }
        } else {
            result.setSuccess(false);
            result.setCode(3);
            result.setMsg("未入网");
        }
        return result;
    }

    /**
     * 请求入网接口
     *
     * @return 处理结果
     */
    @ApiOperation(value = "请求入网接口", notes = "通过收集POS硬件信息请求入网，另外还必须传入网点名称", produces = "application/json")
    @RequestMapping(value = "/join", method = RequestMethod.POST)
    @RequiresUser
    public ResponseEntity<BaseResponse> doJoin(@RequestHeader(name="Content-Type", defaultValue = "application/json") String contentType,
                                               @RequestHeader(name="Authorization", defaultValue="token") String token,
                                               @RequestBody PosParam posParam) {
        _logger.info("请求入网接口 start....");
        BaseResponse result = new BaseResponse();
        // imei码约束检查
        if (StringUtils.isEmpty(posParam.getImei()) || posParam.getImei().length() > 32) {
            result.setSuccess(false);
            result.setMsg("IMEI码长度不是1-32位，入网失败。");
            return new ResponseEntity<>(result, HttpStatus.OK);
        }
        // 序列号SN约束检查
        if (StringUtils.isEmpty(posParam.getSn()) || posParam.getSn().length() > 64) {
            result.setSuccess(false);
            result.setMsg("序列号长度不是1-64位，入网失败。");
            return new ResponseEntity<>(result, HttpStatus.OK);
        }
        // 机具型号约束检查
        if (StringUtils.isEmpty(posParam.getSeries()) || posParam.getSeries().length() > 32) {
            result.setSuccess(false);
            result.setMsg("机具型号不是1-32位，入网失败。");
            return new ResponseEntity<>(result, HttpStatus.OK);
        }
        // Android版本约束检查
        if (StringUtils.isEmpty(posParam.getAndroidVersion()) || posParam.getAndroidVersion().length() > 32) {
            result.setSuccess(false);
            result.setMsg("Android版本号不是1-32位，入网失败。");
            return new ResponseEntity<>(result, HttpStatus.OK);
        }
        // 版本号约束检查
        if (StringUtils.isEmpty(posParam.getVersion()) || posParam.getVersion().length() > 32) {
            result.setSuccess(false);
            result.setMsg("版本号不是1-32位，入网失败。");
            return new ResponseEntity<>(result, HttpStatus.OK);
        }
        // 归属网点约束检查
        if (StringUtils.isEmpty(posParam.getLocation()) || posParam.getLocation().length() > 64) {
            result.setSuccess(false);
            result.setMsg("归属网点不是1-64位，入网失败。");
            return new ResponseEntity<>(result, HttpStatus.OK);
        }
        // 产权方约束检查
        if (StringUtils.isEmpty(posParam.getOwner()) || posParam.getOwner().length() > 64) {
            result.setSuccess(false);
            result.setMsg("产权方不是1-64位，入网失败。");
            return new ResponseEntity<>(result, HttpStatus.OK);
        }
        // 应用ID约束检查
        if (StringUtils.isEmpty(posParam.getApplicationId()) || posParam.getApplicationId().length() > 64) {
            result.setSuccess(false);
            result.setMsg("应用ID不是1-64位，入网失败。");
            return new ResponseEntity<>(result, HttpStatus.OK);
        }
        // 备注约束检查
        if (StringUtils.isNotEmpty(posParam.getTips()) && posParam.getTips().length() > 255) {
            result.setSuccess(false);
            result.setMsg("备注超过255个字符，入网失败。");
            return new ResponseEntity<>(result, HttpStatus.OK);
        }
        Pos pos = new Pos();
        Date now = new Date();
        pos.setJointime(now);
        pos.setBindtime(now);
        BeanUtils.copyProperties(posParam, pos);
        // 根据applicationId设置归属项目ID
        Project project = apiService.selectProjectByApplicationId(posParam.getApplicationId());
        if (project == null) {
            result.setSuccess(false);
            result.setMsg("Application Id不正确，入网失败。");
            return new ResponseEntity<>(result, HttpStatus.OK);
        }
        // 重复检查
        int posCount = apiService.selectCount(posParam.getImei());
        if (posCount > 0) {
            result.setSuccess(false);
            result.setMsg("入网失败，该机具之前已经入网了。");
            return new ResponseEntity<>(result, HttpStatus.OK);
        }
        // 插入一条新纪录
        pos.setProjectId(project.getId());
        int insert = apiService.insertPos(pos);
        if (insert > 0) {
            result.setSuccess(true);
            result.setMsg("入网成功");
            return new ResponseEntity<>(result, HttpStatus.CREATED);
        } else {
            result.setSuccess(false);
            result.setMsg("入网失败，请联系管理员。");
            return new ResponseEntity<>(result, HttpStatus.OK);
        }
    }

    /**
     * 请求绑定网点接口
     *
     * @return 处理结果
     */
    @ApiOperation(value = "请求绑定网点接口", notes = "POS机重新绑定新的网点", produces = "application/json")
    @RequestMapping(value = "/bind", method = RequestMethod.POST)
//    @RequiresPermissions(logical = Logical.AND, value = {"view", "edit"})
    @RequiresUser
    public ResponseEntity<BaseResponse> doBind(@RequestHeader(name="Content-Type", defaultValue = "application/json") String contentType,
                                               @RequestHeader(name="Authorization", defaultValue="token") String token,
                                               @RequestBody BindParam bindParam) {
        _logger.info("请求绑定网点接口 start....");
        BaseResponse result = new BaseResponse();
        // imei码约束检查
        if (StringUtils.isEmpty(bindParam.getImei()) || bindParam.getImei().length() > 32) {
            result.setSuccess(false);
            result.setMsg("IMEI码长度不是1-32位，绑定网点失败。");
            return new ResponseEntity<>(result, HttpStatus.OK);
        }
        // 归属网点约束检查
        if (StringUtils.isEmpty(bindParam.getLocation()) || bindParam.getLocation().length() > 64) {
            result.setSuccess(false);
            result.setMsg("归属网点不是1-64位，绑定网点失败。");
            return new ResponseEntity<>(result, HttpStatus.OK);
        }

        Pos pos = apiService.selectByImei(bindParam.getImei());
        if (pos == null) {
            result.setSuccess(false);
            result.setMsg("该POS机尚未入网。");
            return new ResponseEntity<>(result, HttpStatus.OK);
        }

        Pos pos2 = apiService.selectBindByImei(bindParam.getImei());
        if (pos2 != null) {
            result.setSuccess(false);
            result.setMsg("该POS机已经绑定了网点，请先解绑。");
            return new ResponseEntity<>(result, HttpStatus.OK);
        }

        pos.setLocation(bindParam.getLocation());
        pos.setUpdatedTime(new Date());
        apiService.bindLocation(pos);

        result.setSuccess(true);
        result.setMsg("绑定网点成功");
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    /**
     * 报告位置接口
     *
     * @return 报告结果
     */
    @ApiOperation(value = "报告位置接口", notes = "终端设备定时报告位置信息", produces = "application/json")
    @RequestMapping(value = "/report", method = RequestMethod.POST)
    @RequiresUser
    public BaseResponse report(@RequestHeader(name="Content-Type", defaultValue = "application/json") String contentType,
                               @RequestHeader(name="Authorization", defaultValue="token") String token,
                               @RequestBody ReportParam param) {
        _logger.info("定时报告接口 start....");
        BaseResponse result = new BaseResponse();
        // IMEI码约束检查
        if (StringUtils.isEmpty(param.getImei()) || param.getImei().length() > 32) {
            result.setSuccess(false);
            result.setMsg("IMEI码不是1-32位，心跳报告失败。");
            return result;
        }
        // 地址约束检查
        if (StringUtils.isEmpty(param.getLocation()) || param.getLocation().length() > 255) {
            result.setSuccess(false);
            result.setMsg("地址不是1-255位，心跳报告失败。");
            return result;
        }

        int r = apiService.report(param);
        if (r > 0) {
            result.setSuccess(true);
            result.setMsg("心跳报告成功");
        } else {
            result.setSuccess(false);
            result.setMsg("该POS机还没有入网，心跳报告失败。");
        }
        return result;
    }

    /**
     * 版本检查接口
     *
     * @return 版本检查结果
     */
    @ApiOperation(value = "APP版本更新检查", notes = "APP版本更新检查", produces = "application/json")
    @RequestMapping(value = "/version", method = RequestMethod.POST)
    @RequiresUser
    public VersionResult version(@RequestHeader(name="Content-Type", defaultValue = "application/json") String contentType,
                                 @RequestHeader(name="Authorization", defaultValue="token") String token,
                                 @RequestBody VersionParam param) {
        _logger.info("版本检查接口 start....");
        return apiService.selectPublishCount(param);
    }

}
