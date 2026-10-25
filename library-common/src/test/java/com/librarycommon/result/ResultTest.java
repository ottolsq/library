package com.librarycommon.result;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 统一响应结构测试。
 */
class ResultTest {

    @Test
    @DisplayName("成功响应携带数据与 200 业务码")
    void ok_carriesDataAndSuccessCode() {
        Result<String> result = Result.ok("data");

        assertThat(result.getCode()).isEqualTo(ResultCode.SUCCESS.getCode());
        assertThat(result.getMessage()).isEqualTo(ResultCode.SUCCESS.getMsg());
        assertThat(result.getData()).isEqualTo("data");
        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    @DisplayName("失败响应不携带数据")
    void fail_hasNoData() {
        Result<Void> result = Result.fail(ResultCode.DATA_NOT_FOUND);

        assertThat(result.getCode()).isEqualTo(ResultCode.DATA_NOT_FOUND.getCode());
        assertThat(result.getMessage()).isEqualTo(ResultCode.DATA_NOT_FOUND.getMsg());
        assertThat(result.getData()).isNull();
        assertThat(result.isSuccess()).isFalse();
    }

    @Test
    @DisplayName("失败响应可覆盖错误提示")
    void fail_canOverrideMessage() {
        Result<Void> result = Result.fail(ResultCode.PARAM_INVALID, "ISBN已存在");

        assertThat(result.getCode()).isEqualTo(ResultCode.PARAM_INVALID.getCode());
        assertThat(result.getMessage()).isEqualTo("ISBN已存在");
    }
}
