/*
 *
 *
 *
 */
package net.eshop.dao.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Pattern;

import javax.annotation.Resource;
import javax.persistence.FlushModeType;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Subquery;

import net.eshop.Filter;
import net.eshop.Order;
import net.eshop.Page;
import net.eshop.Pageable;
import net.eshop.Setting;
import net.eshop.dao.GoodsDao;
import net.eshop.dao.ProductDao;
import net.eshop.dao.SnDao;
import net.eshop.entity.Attribute;
import net.eshop.entity.Brand;
import net.eshop.entity.Goods;
import net.eshop.entity.Member;
import net.eshop.entity.Order.OrderStatus;
import net.eshop.entity.Order.PaymentStatus;
import net.eshop.entity.OrderItem;
import net.eshop.entity.Product;
import net.eshop.entity.Product.OrderType;
import net.eshop.entity.ProductCategory;
import net.eshop.entity.Promotion;
import net.eshop.entity.Sn.Type;
import net.eshop.entity.SpecificationValue;
import net.eshop.entity.Tag;
import net.eshop.util.SettingUtils;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.CompareToBuilder;
import org.springframework.stereotype.Repository;
import org.springframework.util.Assert;


/**
 * Dao - 商品
 *
 *
 *
 */
@Repository("productDaoImpl")
public class ProductDaoImpl extends BaseDaoImpl<Product, Long> implements ProductDao
{

	private static final Pattern pattern = Pattern.compile("\\d*");

	@Resource(name = "goodsDaoImpl")
	private GoodsDao goodsDao;
	@Resource(name = "snDaoImpl")
	private SnDao snDao;

	public boolean snExists(final String sn)
	{
		if (sn == null)
		{
			return false;
		}
		final String jpql = "select count(*) from Product product where lower(product.sn) = lower(:sn)";
		final Long count = entityManager.createQuery(jpql, Long.class).setFlushMode(FlushModeType.COMMIT).setParameter("sn", sn)
				.getSingleResult();
		return count > 0;
	}

	public Product findBySn(final String sn)
	{
		if (sn == null)
		{
			return null;
		}
		final String jpql = "select product from Product product where lower(product.sn) = lower(:sn)";
		try
		{
			return entityManager.createQuery(jpql, Product.class).setFlushMode(FlushModeType.COMMIT).setParameter("sn", sn)
					.getSingleResult();
		}
		catch (final NoResultException e)
		{
			return null;
		}
	}

	public List<Product> search(final String keyword, final Boolean isGift, final Integer count)
	{
		if (StringUtils.isEmpty(keyword))
		{
			return Collections.<Product> emptyList();
		}
		final CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
		final CriteriaQuery<Product> criteriaQuery = criteriaBuilder.createQuery(Product.class);
		final Root<Product> root = criteriaQuery.from(Product.class);
		criteriaQuery.select(root);
		Predicate restrictions = criteriaBuilder.conjunction();
		if (pattern.matcher(keyword).matches())
		{
			restrictions = criteriaBuilder.and(
					restrictions,
					criteriaBuilder.or(criteriaBuilder.equal(root.get("id"), Long.valueOf(keyword)),
							criteriaBuilder.like(root.<String> get("sn"), "%" + keyword + "%"),
							criteriaBuilder.like(root.<String> get("fullName"), "%" + keyword + "%")));
		}
		else
		{
			restrictions = criteriaBuilder.and(
					restrictions,
					criteriaBuilder.or(criteriaBuilder.like(root.<String> get("sn"), "%" + keyword + "%"),
							criteriaBuilder.like(root.<String> get("fullName"), "%" + keyword + "%")));
		}
		if (isGift != null)
		{
			restrictions = criteriaBuilder.and(restrictions, criteriaBuilder.equal(root.get("isGift"), isGift));
		}
		criteriaQuery.where(restrictions);
		criteriaQuery.orderBy(criteriaBuilder.desc(root.get("isTop")));
		return super.findList(criteriaQuery, null, count, null, null);
	}

	public List<Product> findList(final ProductCategory productCategory, final Brand brand, final Promotion promotion,
			final List<Tag> tags, final Map<Attribute, String> attributeValue, BigDecimal startPrice, BigDecimal endPrice,
			final Boolean isMarketable, final Boolean isList, final Boolean isTop, final Boolean isGift, final Boolean isOutOfStock,
			final Boolean isStockAlert, final OrderType orderType, final Integer count, final List<Filter> filters,
			final List<Order> orders)
	{
		final CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
		final CriteriaQuery<Product> criteriaQuery = criteriaBuilder.createQuery(Product.class);
		final Root<Product> root = criteriaQuery.from(Product.class);
		criteriaQuery.select(root);
		Predicate restrictions = criteriaBuilder.conjunction();
		if (productCategory != null)
		{
			restrictions = criteriaBuilder.and(
					restrictions,
					criteriaBuilder.or(
							criteriaBuilder.equal(root.get("productCategory"), productCategory),
							criteriaBuilder.like(root.get("productCategory").<String> get("treePath"), "%"
									+ ProductCategory.TREE_PATH_SEPARATOR + productCategory.getId() + ProductCategory.TREE_PATH_SEPARATOR
									+ "%")));
		}
		if (brand != null)
		{
			restrictions = criteriaBuilder.and(restrictions, criteriaBuilder.equal(root.get("brand"), brand));
		}
		if (promotion != null)
		{
			final Subquery<Product> subquery1 = criteriaQuery.subquery(Product.class);
			final Root<Product> subqueryRoot1 = subquery1.from(Product.class);
			subquery1.select(subqueryRoot1);
			subquery1.where(criteriaBuilder.equal(subqueryRoot1, root),
					criteriaBuilder.equal(subqueryRoot1.join("promotions"), promotion));

			final Subquery<Product> subquery2 = criteriaQuery.subquery(Product.class);
			final Root<Product> subqueryRoot2 = subquery2.from(Product.class);
			subquery2.select(subqueryRoot2);
			subquery2.where(criteriaBuilder.equal(subqueryRoot2, root),
					criteriaBuilder.equal(subqueryRoot2.join("productCategory").join("promotions"), promotion));

			final Subquery<Product> subquery3 = criteriaQuery.subquery(Product.class);
			final Root<Product> subqueryRoot3 = subquery3.from(Product.class);
			subquery3.select(subqueryRoot3);
			subquery3.where(criteriaBuilder.equal(subqueryRoot3, root),
					criteriaBuilder.equal(subqueryRoot3.join("brand").join("promotions"), promotion));

			restrictions = criteriaBuilder.and(
					restrictions,
					criteriaBuilder.or(criteriaBuilder.exists(subquery1), criteriaBuilder.exists(subquery2),
							criteriaBuilder.exists(subquery3)));
		}
		if (tags != null && !tags.isEmpty())
		{
			final Subquery<Product> subquery = criteriaQuery.subquery(Product.class);
			final Root<Product> subqueryRoot = subquery.from(Product.class);
			subquery.select(subqueryRoot);
			subquery.where(criteriaBuilder.equal(subqueryRoot, root), subqueryRoot.join("tags").in(tags));
			restrictions = criteriaBuilder.and(restrictions, criteriaBuilder.exists(subquery));
		}
		if (attributeValue != null)
		{
			for (final Entry<Attribute, String> entry : attributeValue.entrySet())
			{
				final String propertyName = Product.ATTRIBUTE_VALUE_PROPERTY_NAME_PREFIX + entry.getKey().getPropertyIndex();
				restrictions = criteriaBuilder.and(restrictions, criteriaBuilder.equal(root.get(propertyName), entry.getValue()));
			}
		}
		if (startPrice != null && endPrice != null && startPrice.compareTo(endPrice) > 0)
		{
			final BigDecimal temp = startPrice;
			startPrice = endPrice;
			endPrice = temp;
		}
		if (startPrice != null && startPrice.compareTo(new BigDecimal(0)) >= 0)
		{
			restrictions = criteriaBuilder.and(restrictions, criteriaBuilder.ge(root.<Number> get("price"), startPrice));
		}
		if (endPrice != null && endPrice.compareTo(new BigDecimal(0)) >= 0)
		{
			restrictions = criteriaBuilder.and(restrictions, criteriaBuilder.le(root.<Number> get("price"), endPrice));
		}
		if (isMarketable != null)
		{
			restrictions = criteriaBuilder.and(restrictions, criteriaBuilder.equal(root.get("isMarketable"), isMarketable));
		}
		if (isList != null)
		{
			restrictions = criteriaBuilder.and(restrictions, criteriaBuilder.equal(root.get("isList"), isList));
		}
		if (isTop != null)
		{
			restrictions = criteriaBuilder.and(restrictions, criteriaBuilder.equal(root.get("isTop"), isTop));
		}
		if (isGift != null)
		{
			restrictions = criteriaBuilder.and(restrictions, criteriaBuilder.equal(root.get("isGift"), isGift));
		}
		final Path<Integer> stock = root.get("stock");
		final Path<Integer> allocatedStock = root.get("allocatedStock");
		if (isOutOfStock != null)
		{
			if (isOutOfStock)
			{
				restrictions = criteriaBuilder.and(restrictions, criteriaBuilder.isNotNull(stock),
						criteriaBuilder.lessThanOrEqualTo(stock, allocatedStock));
			}
			else
			{
				restrictions = criteriaBuilder.and(restrictions,
						criteriaBuilder.or(criteriaBuilder.isNull(stock), criteriaBuilder.greaterThan(stock, allocatedStock)));
			}
		}
		if (isStockAlert != null)
		{
			final Setting setting = SettingUtils.get();
			if (isStockAlert)
			{
				restrictions = criteriaBuilder.and(restrictions, criteriaBuilder.isNotNull(stock),
						criteriaBuilder.lessThanOrEqualTo(stock, criteriaBuilder.sum(allocatedStock, setting.getStockAlertCount())));
			}
			else
			{
				restrictions = criteriaBuilder.and(
						restrictions,
						criteriaBuilder.or(criteriaBuilder.isNull(stock),
								criteriaBuilder.greaterThan(stock, criteriaBuilder.sum(allocatedStock, setting.getStockAlertCount()))));
			}
		}
		criteriaQuery.where(restrictions);
		if (orderType == OrderType.priceAsc)
		{
			orders.add(Order.asc("price"));
			orders.add(Order.desc("createDate"));
		}
		else if (orderType == OrderType.priceDesc)
		{
			orders.add(Order.desc("price"));
			orders.add(Order.desc("createDate"));
		}
		else if (orderType == OrderType.salesDesc)
		{
			orders.add(Order.desc("sales"));
			orders.add(Order.desc("createDate"));
		}
		else if (orderType == OrderType.scoreDesc)
		{
			orders.add(Order.desc("score"));
			orders.add(Order.desc("createDate"));
		}
		else if (orderType == OrderType.dateDesc)
		{
			orders.add(Order.desc("createDate"));
		}
		else
		{
			orders.add(Order.desc("isTop"));
			orders.add(Order.desc("modifyDate"));
		}
		return super.findList(criteriaQuery, null, count, filters, orders);
	}

	public List<Product> findList(final ProductCategory productCategory, final Date beginDate, final Date endDate,
			final Integer first, final Integer count)
	{
		final CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
		final CriteriaQuery<Product> criteriaQuery = criteriaBuilder.createQuery(Product.class);
		final Root<Product> root = criteriaQuery.from(Product.class);
		criteriaQuery.select(root);
		Predicate restrictions = criteriaBuilder.conjunction();
		restrictions = criteriaBuilder.and(restrictions, criteriaBuilder.equal(root.get("isMarketable"), true));
		if (productCategory != null)
		{
			restrictions = criteriaBuilder.and(
					restrictions,
					criteriaBuilder.or(
							criteriaBuilder.equal(root.get("productCategory"), productCategory),
							criteriaBuilder.like(root.get("productCategory").<String> get("treePath"), "%"
									+ ProductCategory.TREE_PATH_SEPARATOR + productCategory.getId() + ProductCategory.TREE_PATH_SEPARATOR
									+ "%")));
		}
		if (beginDate != null)
		{
			restrictions = criteriaBuilder.and(restrictions,
					criteriaBuilder.greaterThanOrEqualTo(root.<Date> get("createDate"), beginDate));
		}
		if (endDate != null)
		{
			restrictions = criteriaBuilder.and(restrictions,
					criteriaBuilder.lessThanOrEqualTo(root.<Date> get("createDate"), endDate));
		}
		criteriaQuery.where(restrictions);
		criteriaQuery.orderBy(criteriaBuilder.desc(root.get("isTop")));
		return super.findList(criteriaQuery, first, count, null, null);
	}

	public List<Product> findList(final Goods goods, final Set<Product> excludes)
	{
		final CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
		final CriteriaQuery<Product> criteriaQuery = criteriaBuilder.createQuery(Product.class);
		final Root<Product> root = criteriaQuery.from(Product.class);
		criteriaQuery.select(root);
		Predicate restrictions = criteriaBuilder.conjunction();
		if (goods != null)
		{
			restrictions = criteriaBuilder.and(restrictions, criteriaBuilder.equal(root.get("goods"), goods));
		}
		if (excludes != null && !excludes.isEmpty())
		{
			restrictions = criteriaBuilder.and(restrictions, criteriaBuilder.not(root.in(excludes)));
		}
		criteriaQuery.where(restrictions);
		return entityManager.createQuery(criteriaQuery).setFlushMode(FlushModeType.COMMIT).getResultList();
	}

	public List<Object[]> findSalesList(final Date beginDate, final Date endDate, final Integer count)
	{
		final CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
		final CriteriaQuery<Object[]> criteriaQuery = criteriaBuilder.createQuery(Object[].class);
		final Root<Product> product = criteriaQuery.from(Product.class);
		final Join<Product, OrderItem> orderItems = product.join("orderItems");
		final Join<Product, net.eshop.entity.Order> order = orderItems.join("order");
		criteriaQuery
				.multiselect(
						product.get("id"),
						product.get("sn"),
						product.get("name"),
						product.get("fullName"),
						product.get("price"),
						criteriaBuilder.sum(orderItems.<Integer> get("quantity")),
						criteriaBuilder.sum(criteriaBuilder.prod(orderItems.<Integer> get("quantity"),
								orderItems.<BigDecimal> get("price"))));
		Predicate restrictions = criteriaBuilder.conjunction();
		if (beginDate != null)
		{
			restrictions = criteriaBuilder.and(restrictions,
					criteriaBuilder.greaterThanOrEqualTo(order.<Date> get("createDate"), beginDate));
		}
		if (endDate != null)
		{
			restrictions = criteriaBuilder.and(restrictions,
					criteriaBuilder.lessThanOrEqualTo(order.<Date> get("createDate"), endDate));
		}
		restrictions = criteriaBuilder.and(restrictions, criteriaBuilder.equal(order.get("orderStatus"), OrderStatus.completed),
				criteriaBuilder.equal(order.get("paymentStatus"), PaymentStatus.paid));
		criteriaQuery.where(restrictions);
		criteriaQuery.groupBy(product.get("id"), product.get("sn"), product.get("name"), product.get("fullName"),
				product.get("price"));
		criteriaQuery.orderBy(criteriaBuilder.desc(criteriaBuilder.sum(criteriaBuilder.prod(orderItems.<Integer> get("quantity"),
				orderItems.<BigDecimal> get("price")))));
		final TypedQuery<Object[]> query = entityManager.createQuery(criteriaQuery).setFlushMode(FlushModeType.COMMIT);
		if (count != null && count >= 0)
		{
			query.setMaxResults(count);
		}
		return query.getResultList();
	}

	public Page<Product> findPage(final ProductCategory productCategory, final Brand brand, final Promotion promotion,
			final List<Tag> tags, final Map<Attribute, String> attributeValue, BigDecimal startPrice, BigDecimal endPrice,
			final Boolean isBaseProduct, final Boolean isMarketable, final Boolean isList, final Boolean isTop,
			final Boolean isGift, final Boolean isOutOfStock, final Boolean isStockAlert, final OrderType orderType,
			final Pageable pageable)
	{
		final CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
		final CriteriaQuery<Product> criteriaQuery = criteriaBuilder.createQuery(Product.class);
		final Root<Product> root = criteriaQuery.from(Product.class);
		criteriaQuery.select(root);
		Predicate restrictions = criteriaBuilder.conjunction();
		if (productCategory != null)
		{
			restrictions = criteriaBuilder.and(
					restrictions,
					criteriaBuilder.or(
							criteriaBuilder.equal(root.get("productCategory"), productCategory),
							criteriaBuilder.like(root.get("productCategory").<String> get("treePath"), "%"
									+ ProductCategory.TREE_PATH_SEPARATOR + productCategory.getId() + ProductCategory.TREE_PATH_SEPARATOR
									+ "%")));
		}
		if (brand != null)
		{
			restrictions = criteriaBuilder.and(restrictions, criteriaBuilder.equal(root.get("brand"), brand));
		}
		if (promotion != null)
		{
			final Subquery<Product> subquery1 = criteriaQuery.subquery(Product.class);
			final Root<Product> subqueryRoot1 = subquery1.from(Product.class);
			subquery1.select(subqueryRoot1);
			subquery1.where(criteriaBuilder.equal(subqueryRoot1, root),
					criteriaBuilder.equal(subqueryRoot1.join("promotions"), promotion));

			final Subquery<Product> subquery2 = criteriaQuery.subquery(Product.class);
			final Root<Product> subqueryRoot2 = subquery2.from(Product.class);
			subquery2.select(subqueryRoot2);
			subquery2.where(criteriaBuilder.equal(subqueryRoot2, root),
					criteriaBuilder.equal(subqueryRoot2.join("productCategory").join("promotions"), promotion));

			final Subquery<Product> subquery3 = criteriaQuery.subquery(Product.class);
			final Root<Product> subqueryRoot3 = subquery3.from(Product.class);
			subquery3.select(subqueryRoot3);
			subquery3.where(criteriaBuilder.equal(subqueryRoot3, root),
					criteriaBuilder.equal(subqueryRoot3.join("brand").join("promotions"), promotion));

			restrictions = criteriaBuilder.and(
					restrictions,
					criteriaBuilder.or(criteriaBuilder.exists(subquery1), criteriaBuilder.exists(subquery2),
							criteriaBuilder.exists(subquery3)));
		}
		if (tags != null && !tags.isEmpty())
		{
			final Subquery<Product> subquery = criteriaQuery.subquery(Product.class);
			final Root<Product> subqueryRoot = subquery.from(Product.class);
			subquery.select(subqueryRoot);
			subquery.where(criteriaBuilder.equal(subqueryRoot, root), subqueryRoot.join("tags").in(tags));
			restrictions = criteriaBuilder.and(restrictions, criteriaBuilder.exists(subquery));
		}
		if (attributeValue != null)
		{
			for (final Entry<Attribute, String> entry : attributeValue.entrySet())
			{
				final String propertyName = Product.ATTRIBUTE_VALUE_PROPERTY_NAME_PREFIX + entry.getKey().getPropertyIndex();
				restrictions = criteriaBuilder.and(restrictions, criteriaBuilder.equal(root.get(propertyName), entry.getValue()));
			}
		}
		if (startPrice != null && endPrice != null && startPrice.compareTo(endPrice) > 0)
		{
			final BigDecimal temp = startPrice;
			startPrice = endPrice;
			endPrice = temp;
		}
		if (startPrice != null && startPrice.compareTo(new BigDecimal(0)) >= 0)
		{
			restrictions = criteriaBuilder.and(restrictions, criteriaBuilder.ge(root.<Number> get("price"), startPrice));
		}
		if (endPrice != null && endPrice.compareTo(new BigDecimal(0)) >= 0)
		{
			restrictions = criteriaBuilder.and(restrictions, criteriaBuilder.le(root.<Number> get("price"), endPrice));
		}
		if (isBaseProduct != null)
		{
			restrictions = criteriaBuilder.and(restrictions, criteriaBuilder.equal(root.get("isBaseProduct"), isBaseProduct));
		}
		if (isMarketable != null)
		{
			restrictions = criteriaBuilder.and(restrictions, criteriaBuilder.equal(root.get("isMarketable"), isMarketable));
		}
		if (isList != null)
		{
			restrictions = criteriaBuilder.and(restrictions, criteriaBuilder.equal(root.get("isList"), isList));
		}
		if (isTop != null)
		{
			restrictions = criteriaBuilder.and(restrictions, criteriaBuilder.equal(root.get("isTop"), isTop));
		}
		if (isGift != null)
		{
			restrictions = criteriaBuilder.and(restrictions, criteriaBuilder.equal(root.get("isGift"), isGift));
		}
		final Path<Integer> stock = root.get("stock");
		final Path<Integer> allocatedStock = root.get("allocatedStock");
		if (isOutOfStock != null)
		{
			if (isOutOfStock)
			{
				restrictions = criteriaBuilder.and(restrictions, criteriaBuilder.isNotNull(stock),
						criteriaBuilder.lessThanOrEqualTo(stock, allocatedStock));
			}
			else
			{
				restrictions = criteriaBuilder.and(restrictions,
						criteriaBuilder.or(criteriaBuilder.isNull(stock), criteriaBuilder.greaterThan(stock, allocatedStock)));
			}
		}
		if (isStockAlert != null)
		{
			final Setting setting = SettingUtils.get();
			if (isStockAlert)
			{
				restrictions = criteriaBuilder.and(restrictions, criteriaBuilder.isNotNull(stock),
						criteriaBuilder.lessThanOrEqualTo(stock, criteriaBuilder.sum(allocatedStock, setting.getStockAlertCount())));
			}
			else
			{
				restrictions = criteriaBuilder.and(
						restrictions,
						criteriaBuilder.or(criteriaBuilder.isNull(stock),
								criteriaBuilder.greaterThan(stock, criteriaBuilder.sum(allocatedStock, setting.getStockAlertCount()))));
			}
		}
		criteriaQuery.where(restrictions);
		final List<Order> orders = pageable.getOrders();
		if (orderType == OrderType.priceAsc)
		{
			orders.add(Order.asc("price"));
			orders.add(Order.desc("createDate"));
		}
		else if (orderType == OrderType.priceDesc)
		{
			orders.add(Order.desc("price"));
			orders.add(Order.desc("createDate"));
		}
		else if (orderType == OrderType.salesDesc)
		{
			orders.add(Order.desc("sales"));
			orders.add(Order.desc("createDate"));
		}
		else if (orderType == OrderType.scoreDesc)
		{
			orders.add(Order.desc("score"));
			orders.add(Order.desc("createDate"));
		}
		else if (orderType == OrderType.dateDesc)
		{
			orders.add(Order.desc("createDate"));
		}
		else
		{
			orders.add(Order.desc("isTop"));
			orders.add(Order.desc("modifyDate"));
		}
		return super.findPage(criteriaQuery, pageable);
	}

	public Page<Product> findPage(final Member member, final Pageable pageable)
	{
		if (member == null)
		{
			return new Page<Product>(Collections.<Product> emptyList(), 0, pageable);
		}
		final CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
		final CriteriaQuery<Product> criteriaQuery = criteriaBuilder.createQuery(Product.class);
		final Root<Product> root = criteriaQuery.from(Product.class);
		criteriaQuery.select(root);
		criteriaQuery.where(criteriaBuilder.equal(root.join("favoriteMembers"), member));
		return super.findPage(criteriaQuery, pageable);
	}

	public Long count(final Member favoriteMember, final Boolean isMarketable, final Boolean isList, final Boolean isTop,
			final Boolean isGift, final Boolean isOutOfStock, final Boolean isStockAlert)
	{
		final CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
		final CriteriaQuery<Product> criteriaQuery = criteriaBuilder.createQuery(Product.class);
		final Root<Product> root = criteriaQuery.from(Product.class);
		criteriaQuery.select(root);
		Predicate restrictions = criteriaBuilder.conjunction();
		if (favoriteMember != null)
		{
			restrictions = criteriaBuilder.and(restrictions, criteriaBuilder.equal(root.join("favoriteMembers"), favoriteMember));
		}
		if (isMarketable != null)
		{
			restrictions = criteriaBuilder.and(restrictions, criteriaBuilder.equal(root.get("isMarketable"), isMarketable));
		}
		if (isList != null)
		{
			restrictions = criteriaBuilder.and(restrictions, criteriaBuilder.equal(root.get("isList"), isList));
		}
		if (isTop != null)
		{
			restrictions = criteriaBuilder.and(restrictions, criteriaBuilder.equal(root.get("isTop"), isTop));
		}
		if (isGift != null)
		{
			restrictions = criteriaBuilder.and(restrictions, criteriaBuilder.equal(root.get("isGift"), isGift));
		}
		final Path<Integer> stock = root.get("stock");
		final Path<Integer> allocatedStock = root.get("allocatedStock");
		if (isOutOfStock != null)
		{
			if (isOutOfStock)
			{
				restrictions = criteriaBuilder.and(restrictions, criteriaBuilder.isNotNull(stock),
						criteriaBuilder.lessThanOrEqualTo(stock, allocatedStock));
			}
			else
			{
				restrictions = criteriaBuilder.and(restrictions,
						criteriaBuilder.or(criteriaBuilder.isNull(stock), criteriaBuilder.greaterThan(stock, allocatedStock)));
			}
		}
		if (isStockAlert != null)
		{
			final Setting setting = SettingUtils.get();
			if (isStockAlert)
			{
				restrictions = criteriaBuilder.and(restrictions, criteriaBuilder.isNotNull(stock),
						criteriaBuilder.lessThanOrEqualTo(stock, criteriaBuilder.sum(allocatedStock, setting.getStockAlertCount())));
			}
			else
			{
				restrictions = criteriaBuilder.and(
						restrictions,
						criteriaBuilder.or(criteriaBuilder.isNull(stock),
								criteriaBuilder.greaterThan(stock, criteriaBuilder.sum(allocatedStock, setting.getStockAlertCount()))));
			}
		}
		criteriaQuery.where(restrictions);
		return super.count(criteriaQuery, null);
	}

	public boolean isPurchased(final Member member, final Product product)
	{
		if (member == null || product == null)
		{
			return false;
		}
		final String jqpl = "select count(*) from OrderItem orderItem where orderItem.product = :product and orderItem.order.member = :member and orderItem.order.orderStatus = :orderStatus";
		final Long count = entityManager.createQuery(jqpl, Long.class).setFlushMode(FlushModeType.COMMIT)
				.setParameter("product", product).setParameter("member", member).setParameter("orderStatus", OrderStatus.completed)
				.getSingleResult();
		return count > 0;
	}

	/**
	 * 设置值并保存
	 *
	 * @param product
	 *           商品
	 */
	@Override
	public void persist(final Product product)
	{
		Assert.notNull(product);

		setValue(product);
		super.persist(product);
	}

	/**
	 * 设置值并更新
	 *
	 * @param product
	 *           商品
	 * @return 商品
	 */
	@Override
	public Product merge(final Product product)
	{
		Assert.notNull(product);

		if (!product.getIsGift())
		{
			final String jpql = "delete from GiftItem giftItem where giftItem.gift = :product";
			entityManager.createQuery(jpql).setFlushMode(FlushModeType.COMMIT).setParameter("product", product).executeUpdate();
		}
		if (!product.getIsMarketable() || product.getIsGift())
		{
			final String jpql = "delete from CartItem cartItem where cartItem.product = :product";
			entityManager.createQuery(jpql).setFlushMode(FlushModeType.COMMIT).setParameter("product", product).executeUpdate();
		}
		setValue(product);
		return super.merge(product);
	}

	@Override
	public void remove(final Product product)
	{
		if (product != null)
		{
			final Goods goods = product.getGoods();
			if (goods != null && goods.getProducts() != null)
			{
				goods.getProducts().remove(product);
				if (goods.getProducts().isEmpty())
				{
					goodsDao.remove(goods);
				}
			}
		}
		super.remove(product);
	}

	/**
	 * 设置值
	 *
	 * @param product
	 *           商品
	 */
	private void setValue(final Product product)
	{
		if (product == null)
		{
			return;
		}
		if (StringUtils.isEmpty(product.getSn()))
		{
			String sn;
			do
			{
				sn = snDao.generate(Type.product);
			}
			while (snExists(sn));
			product.setSn(sn);
		}
		final StringBuffer fullName = new StringBuffer(product.getName());
		if (product.getSpecificationValues() != null && !product.getSpecificationValues().isEmpty())
		{
			final List<SpecificationValue> specificationValues = new ArrayList<SpecificationValue>(product.getSpecificationValues());
			Collections.sort(specificationValues, new Comparator<SpecificationValue>()
			{
				public int compare(final SpecificationValue a1, final SpecificationValue a2)
				{
					return new CompareToBuilder().append(a1.getSpecification(), a2.getSpecification()).toComparison();
				}
			});
			fullName.append(Product.FULL_NAME_SPECIFICATION_PREFIX);
			int i = 0;
			for (final Iterator<SpecificationValue> iterator = specificationValues.iterator(); iterator.hasNext(); i++)
			{
				if (i != 0)
				{
					fullName.append(Product.FULL_NAME_SPECIFICATION_SEPARATOR);
				}
				fullName.append(iterator.next().getName());
			}
			fullName.append(Product.FULL_NAME_SPECIFICATION_SUFFIX);
		}
		product.setFullName(fullName.toString());
	}

}