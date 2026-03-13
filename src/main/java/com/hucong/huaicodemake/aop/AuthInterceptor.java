package com.hucong.huaicodemake.aop;

import com.hucong.huaicodemake.annotation.AuthCheck;
import com.hucong.huaicodemake.exception.BusinessException;
import com.hucong.huaicodemake.exception.ErrorCode;
import com.hucong.huaicodemake.model.entity.User;
import com.hucong.huaicodemake.model.enums.UserRoleEnum;
import com.hucong.huaicodemake.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
public class AuthInterceptor {
    @Resource
    private UserService userService;

    /**
     * 执行拦截
     *
     * @param joinPoint 切点
     * @param authCheck 权限校验注解
     * @return 执行结果
     * @Around("@annotation(authCheck)") 拦截指定注解
     */
    @Around("@annotation(authCheck)")
    /**
     * 权限校验拦截方法，验证当前登录用户是否具有指定角色权限
     *
     * @param joinPoint AOP 切面连接点，用于执行原方法
     * @param authCheck 权限校验注解，包含必须的角色信息
     * @return 原方法的执行结果
     * @throws Throwable 方法执行过程中的异常
     */
    public Object doInterceptor(ProceedingJoinPoint joinPoint, AuthCheck authCheck) throws Throwable {
        String mustRole = authCheck.mustRole();
        RequestAttributes requestAttributes = RequestContextHolder.currentRequestAttributes();
        HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();

        // 获取当前登录用户信息
        User loginUser = userService.getLoginUser(request);

        // 根据注解配置获取要求的角色枚举
        UserRoleEnum mustRoleEnum = UserRoleEnum.getEnumByValue(mustRole);

        // 不需要权限校验，直接放行
        if (mustRoleEnum == null) {
            return joinPoint.proceed();
        }

        // 获取当前用户的角色枚举
        UserRoleEnum userRoleEnum = UserRoleEnum.getEnumByValue(loginUser.getUserRole());

        // 用户角色无效或无权限时抛出业务异常
        if (userRoleEnum == null || !hasPermission(userRoleEnum, mustRoleEnum)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限访问");
        }

        return joinPoint.proceed();
    }

    /**
     * 判断用户是否拥有指定角色权限
     * <p>管理员拥有所有权限，普通用户需匹配对应角色</p>
     *
     * @param userRole 当前用户的角色
     * @param requiredRole 接口要求的角色
     * @return true-有权限，false-无权限
     */
    private boolean hasPermission(UserRoleEnum userRole, UserRoleEnum requiredRole) {
        // 管理员角色拥有所有权限
        if (UserRoleEnum.ADMIN.equals(userRole)) {
            return true;
        }
        return userRole == requiredRole;
    }



}
