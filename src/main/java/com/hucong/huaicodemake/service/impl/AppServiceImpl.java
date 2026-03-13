package com.hucong.huaicodemake.service.impl;

import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.hucong.huaicodemake.model.entity.App;
import com.hucong.huaicodemake.mapper.AppMapper;
import com.hucong.huaicodemake.service.AppService;
import org.springframework.stereotype.Service;

/**
 * 应用 服务层实现。
 *
 * @author <a href="https://github.com/kiiito">程序员hucong</a>
 */
@Service
public class AppServiceImpl extends ServiceImpl<AppMapper, App>  implements AppService{

}
