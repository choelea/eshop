/*
 * 
 * 
 * 
 */
package net.eshop.service;

import net.eshop.entity.ProductImage;


/**
 * Service - 商品图片
 * 
 * 
 * 
 */
public interface ProductImageService
{

	/**
	 * 生成商品图片
	 * 
	 * @param productImage
	 *           商品图片
	 */
	void build(ProductImage productImage);

}