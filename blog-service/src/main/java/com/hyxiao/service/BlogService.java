package com.hyxiao.service;

import com.hyxiao.blog.dto.BlogDTO;
import com.hyxiao.blog.dto.BlogListDTO;
import com.hyxiao.blog.dto.BlogQueryDTO;
import com.hyxiao.blog.entity.BlogEntity;
import com.hyxiao.blog.repo.BlogRepository;
import com.hyxiao.utils.RedisOperator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Hyxiao
 * @date 12/08/2023 20:11
 * @description
 */
@Service
@Slf4j
public class BlogService {

    private String BLOG_VIEWCOUNT_KEY = "blog:viewcount:";
    private String BLOG_LIKE_KEY = "blog_like_count_";

    @Autowired
    private BlogRepository blogRepository;

    @Autowired
    private RedisOperator redisOperator;

    /**
     * 获取所有博客
     *
     * @return 博客列表
     */
    public BlogListDTO getBlogsByQuery(BlogQueryDTO blogQueryDTO) {
        log.info("blogQueryDTO: {}", blogQueryDTO);
        // 构建 Pageable 对象, 用于分页查询
        Pageable pageable = PageRequest.of(blogQueryDTO.getPage() - 1, blogQueryDTO.getPageSize(), Sort.by("createdTime").descending());
        // 构建查询条件
        Specification<BlogEntity> specification = (root, criteriaQuery, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            // 根据分类查询
            if (blogQueryDTO.getCategory() != null && !"".equals(blogQueryDTO.getCategory())) {
                predicates.add(criteriaBuilder.equal(root.get("category"), blogQueryDTO.getCategory()));
            }
            // 根据关键字查询
            if (blogQueryDTO.getKeyword() != null && !"".equals(blogQueryDTO.getKeyword())) {
                log.info("keyword: {}", blogQueryDTO.getKeyword());
                predicates.add(criteriaBuilder.like(root.get("title"), "%" + blogQueryDTO.getKeyword() + "%"));
            }
            return criteriaBuilder.and(predicates.toArray(new Predicate[predicates.size()]));
        };

        // 执行分页查询
        Page<BlogEntity> blogPage = blogRepository.findAll(specification, pageable);

        BlogListDTO blogListDTO = new BlogListDTO();
        blogListDTO.setPage(blogQueryDTO.getPage());
        blogListDTO.setRows(blogPage.map(BlogDTO::convertFrom).getContent());
        blogListDTO.setTotal(blogPage.getTotalElements());

        return blogListDTO;
    }

    /**
     * 获取博客列表
     *
     * @return 博客列表
     */
    public BlogListDTO getTopBlogs() {
        List<BlogEntity> topBlogs = blogRepository.findTopBlogs();
        BlogListDTO blogListDTO = new BlogListDTO();
        blogListDTO.setRows(topBlogs);
        return blogListDTO;
    }

    /**
     * 获取博客分类
     *
     * @return 博客分类
     */
    public Map<String, Long> getBlogCategory() {
        List<Object[]> categorys = blogRepository.countTotalPostsByCategory();

        return categorys.stream().collect(Collectors.toMap(category -> (String) category[0], category -> (Long) category[1]));
    }

    /**
     * 获取根据浏览量排序前十的博客
     *
     * @return 博客
     */
    public BlogDTO getBlogById(String host, Long id) {
        BlogEntity blog = blogRepository.findById(id).orElse(null);
        assert blog != null;
        String key = BLOG_LIKE_KEY + host + "_" + id;
        boolean isExist = this.validKeyIsExist(key);
        BlogDTO blogDTO = BlogDTO.convertFrom(blog);
        blogDTO.setIsLiked(isExist);
        return blogDTO;
    }

    /**
     * 创建博客
     */
    public void createBlog(BlogDTO blog) {
        BlogEntity blogEntity = new BlogEntity();
        blogEntity.setTitle(blog.getTitle());
        blogEntity.setCategory(blog.getCategory());
        blogEntity.setCreatedTime(new Date());
        blogEntity.setUpdatedTime(new Date());
        blogEntity.setLikes(blog.getLikes());
        blogEntity.setComments(blog.getComments());
        this.blogRepository.save(blogEntity);
    }

    /**
     * 更新博客
     */
    public void updateBlog(BlogDTO blog) {
        BlogEntity blogEntity = new BlogEntity();
        blogEntity.setId(blog.getId());
        blogEntity.setTitle(blog.getTitle());
        blogEntity.setCategory(blog.getCategory());
        blogEntity.setCreatedTime(blog.getCreatedTime());
        blogEntity.setUpdatedTime(blog.getUpdatedTime());
        blogEntity.setLikes(blog.getLikes());
        blogEntity.setComments(blog.getComments());
        this.blogRepository.save(blogEntity);
    }

    /**
     * 删除博客
     */
    public void deleteBlog(Long id) {
        this.blogRepository.deleteById(id);
    }

    /**
     * 增加浏览数
     */
    public void incrementView(Long id) {
        this.redisOperator.increment(BLOG_VIEWCOUNT_KEY + id, 1);
    }

    public Boolean incrementLike(String host, Long id) {
        String key = BLOG_LIKE_KEY + host + "_" + id;
        boolean isExist = this.validKeyIsExist(key);
        if (!isExist) {
            this.redisOperator.increment(BLOG_LIKE_KEY + id, 1);
            this.redisOperator.set(key, "7 天内只能记为一次点赞", 60 * 60 * 24 * 7);
            return true;
        } else {
            this.redisOperator.del(key);
            this.redisOperator.decrement(BLOG_LIKE_KEY + id, 1);
            return false;
        }
    }

    public Boolean validKeyIsExist(String key) {
        return this.redisOperator.keyIsExist(key);
    }

}