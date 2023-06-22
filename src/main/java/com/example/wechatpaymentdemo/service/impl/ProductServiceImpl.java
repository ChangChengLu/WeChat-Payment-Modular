package com.example.wechatpaymentdemo.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.wechatpaymentdemo.model.entity.Product;
import com.example.wechatpaymentdemo.service.ProductService;
import com.example.wechatpaymentdemo.mapper.ProductMapper;
import org.springframework.stereotype.Service;

/**
* @author 21237
* @description 针对表【t_product】的数据库操作Service实现
* @createDate 2023-06-03 14:10:14
*/
@Service
public class ProductServiceImpl extends ServiceImpl<ProductMapper, Product>
    implements ProductService{

}




