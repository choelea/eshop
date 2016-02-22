/*
 *
 *
 *
 */
package net.eshop.service.impl;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.persistence.LockModeType;

import net.eshop.Filter;
import net.eshop.Order;
import net.eshop.Page;
import net.eshop.Pageable;
import net.eshop.dao.ProductDao;
import net.eshop.entity.Attribute;
import net.eshop.entity.Brand;
import net.eshop.entity.Member;
import net.eshop.entity.Product;
import net.eshop.entity.Product.OrderType;
import net.eshop.entity.ProductCategory;
import net.eshop.entity.Promotion;
import net.eshop.entity.Tag;
import net.eshop.service.ProductService;
import net.eshop.service.StaticService;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;


/**
 * Service - 商品
 *
 *
 *
 */
@Service("productServiceImpl")
public class ProductServiceImpl extends BaseServiceImpl<Product, Long> implements ProductService, DisposableBean
{

	/** 查看点击数时间 */
	private long viewHitsTime = System.currentTimeMillis();

	@Resource(name = "ehCacheManager")
	private CacheManager cacheManager;
	@Resource(name = "productDaoImpl")
	private ProductDao productDao;
	@Resource(name = "staticServiceImpl")
	private StaticService staticService;

	@Resource(name = "productDaoImpl")
	public void setBaseDao(final ProductDao productDao)
	{
		super.setBaseDao(productDao);
	}

	@Transactional(readOnly = true)
	public boolean snExists(final String sn)
	{
		return productDao.snExists(sn);
	}

	@Transactional(readOnly = true)
	public Product findBySn(final String sn)
	{
		return productDao.findBySn(sn);
	}

	@Transactional(readOnly = true)
	public boolean snUnique(final String previousSn, final String currentSn)
	{
		if (StringUtils.equalsIgnoreCase(previousSn, currentSn))
		{
			return true;
		}
		else
		{
			if (productDao.snExists(currentSn))
			{
				return false;
			}
			else
			{
				return true;
			}
		}
	}

	@Transactional(readOnly = true)
	public List<Product> search(final String keyword, final Boolean isGift, final Integer count)
	{
		return productDao.search(keyword, isGift, count);
	}

	@Transactional(readOnly = true)
	public List<Product> findList(final ProductCategory productCategory, final Brand brand, final Promotion promotion,
			final List<Tag> tags, final Map<Attribute, String> attributeValue, final BigDecimal startPrice,
			final BigDecimal endPrice, final Boolean isMarketable, final Boolean isList, final Boolean isTop, final Boolean isGift,
			final Boolean isOutOfStock, final Boolean isStockAlert, final OrderType orderType, final Integer count,
			final List<Filter> filters, final List<Order> orders)
	{
		return productDao.findList(productCategory, brand, promotion, tags, attributeValue, startPrice, endPrice, isMarketable,
				isList, isTop, isGift, isOutOfStock, isStockAlert, orderType, count, filters, orders);
	}

	@Transactional(readOnly = true)
	@Cacheable("product")
	public List<Product> findList(final ProductCategory productCategory, final Brand brand, final Promotion promotion,
			final List<Tag> tags, final Map<Attribute, String> attributeValue, final BigDecimal startPrice,
			final BigDecimal endPrice, final Boolean isMarketable, final Boolean isList, final Boolean isTop, final Boolean isGift,
			final Boolean isOutOfStock, final Boolean isStockAlert, final OrderType orderType, final Integer count,
			final List<Filter> filters, final List<Order> orders, final String cacheRegion)
	{
		return productDao.findList(productCategory, brand, promotion, tags, attributeValue, startPrice, endPrice, isMarketable,
				isList, isTop, isGift, isOutOfStock, isStockAlert, orderType, count, filters, orders);
	}

	@Transactional(readOnly = true)
	public List<Product> findList(final ProductCategory productCategory, final Date beginDate, final Date endDate,
			final Integer first, final Integer count)
	{
		return productDao.findList(productCategory, beginDate, endDate, first, count);
	}

	@Transactional(readOnly = true)
	public List<Object[]> findSalesList(final Date beginDate, final Date endDate, final Integer count)
	{
		return productDao.findSalesList(beginDate, endDate, count);
	}

	@Transactional(readOnly = true)
	public Page<Product> findPage(final ProductCategory productCategory, final Brand brand, final Promotion promotion,
			final List<Tag> tags, final Map<Attribute, String> attributeValue, final BigDecimal startPrice,
			final BigDecimal endPrice, final Boolean isBaseProduct, final Boolean isMarketable, final Boolean isList,
			final Boolean isTop, final Boolean isGift, final Boolean isOutOfStock, final Boolean isStockAlert,
			final OrderType orderType, final Pageable pageable)
	{
		return productDao.findPage(productCategory, brand, promotion, tags, attributeValue, startPrice, endPrice, isBaseProduct,
				isMarketable, isList, isTop, isGift, isOutOfStock, isStockAlert, orderType, pageable);
	}

	@Transactional(readOnly = true)
	public Page<Product> findPage(final Member member, final Pageable pageable)
	{
		return productDao.findPage(member, pageable);
	}

	@Transactional(readOnly = true)
	public Long count(final Member favoriteMember, final Boolean isMarketable, final Boolean isList, final Boolean isTop,
			final Boolean isGift, final Boolean isOutOfStock, final Boolean isStockAlert)
	{
		return productDao.count(favoriteMember, isMarketable, isList, isTop, isGift, isOutOfStock, isStockAlert);
	}

	@Transactional(readOnly = true)
	public boolean isPurchased(final Member member, final Product product)
	{
		return productDao.isPurchased(member, product);
	}

	public long viewHits(final Long id)
	{
		final Ehcache cache = cacheManager.getEhcache(Product.HITS_CACHE_NAME);
		final Element element = cache.get(id);
		Long hits;
		if (element != null)
		{
			hits = (Long) element.getObjectValue();
		}
		else
		{
			final Product product = productDao.find(id);
			if (product == null)
			{
				return 0L;
			}
			hits = product.getHits();
		}
		hits++;
		cache.put(new Element(id, hits));
		final long time = System.currentTimeMillis();
		if (time > viewHitsTime + Product.HITS_CACHE_INTERVAL)
		{
			viewHitsTime = time;
			updateHits();
			cache.removeAll();
		}
		return hits;
	}

	public void destroy() throws Exception
	{
		updateHits();
	}

	/**
	 * 更新点击数
	 */
	@SuppressWarnings("unchecked")
	private void updateHits()
	{
		final Ehcache cache = cacheManager.getEhcache(Product.HITS_CACHE_NAME);
		final List<Long> ids = cache.getKeys();
		for (final Long id : ids)
		{
			final Product product = productDao.find(id);
			if (product != null)
			{
				productDao.lock(product, LockModeType.PESSIMISTIC_WRITE);
				final Element element = cache.get(id);
				final long hits = (Long) element.getObjectValue();
				final long increment = hits - product.getHits();
				final Calendar nowCalendar = Calendar.getInstance();
				final Calendar weekHitsCalendar = DateUtils.toCalendar(product.getWeekHitsDate());
				final Calendar monthHitsCalendar = DateUtils.toCalendar(product.getMonthHitsDate());
				if (nowCalendar.get(Calendar.YEAR) != weekHitsCalendar.get(Calendar.YEAR)
						|| nowCalendar.get(Calendar.WEEK_OF_YEAR) > weekHitsCalendar.get(Calendar.WEEK_OF_YEAR))
				{
					product.setWeekHits(increment);
				}
				else
				{
					product.setWeekHits(product.getWeekHits() + increment);
				}
				if (nowCalendar.get(Calendar.YEAR) != monthHitsCalendar.get(Calendar.YEAR)
						|| nowCalendar.get(Calendar.MONTH) > monthHitsCalendar.get(Calendar.MONTH))
				{
					product.setMonthHits(increment);
				}
				else
				{
					product.setMonthHits(product.getMonthHits() + increment);
				}
				product.setHits(hits);
				product.setWeekHitsDate(new Date());
				product.setMonthHitsDate(new Date());
				productDao.merge(product);
			}
		}
	}

	@Override
	@Transactional
	@CacheEvict(value =
	{ "product", "productCategory", "review", "consultation" }, allEntries = true)
	public void save(final Product product)
	{
		Assert.notNull(product);

		super.save(product);
		productDao.flush();
		staticService.build(product);
	}

	@Override
	@Transactional
	@CacheEvict(value =
	{ "product", "productCategory", "review", "consultation" }, allEntries = true)
	public Product update(final Product product)
	{
		Assert.notNull(product);

		final Product pProduct = super.update(product);
		productDao.flush();
		staticService.build(pProduct);
		return pProduct;
	}

	@Override
	@Transactional
	@CacheEvict(value =
	{ "product", "productCategory", "review", "consultation" }, allEntries = true)
	public Product update(final Product product, final String... ignoreProperties)
	{
		return super.update(product, ignoreProperties);
	}

	@Override
	@Transactional
	@CacheEvict(value =
	{ "product", "productCategory", "review", "consultation" }, allEntries = true)
	public void delete(final Long id)
	{
		super.delete(id);
	}

	@Override
	@Transactional
	@CacheEvict(value =
	{ "product", "productCategory", "review", "consultation" }, allEntries = true)
	public void delete(final Long... ids)
	{
		super.delete(ids);
	}

	@Override
	@Transactional
	@CacheEvict(value =
	{ "product", "productCategory", "review", "consultation" }, allEntries = true)
	public void delete(final Product product)
	{
		if (product != null)
		{
			staticService.delete(product);
		}
		super.delete(product);
	}

}