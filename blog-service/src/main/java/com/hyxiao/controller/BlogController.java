package com.hyxiao.controller;

import com.hyxiao.blog.dto.BlogDTO;
import com.hyxiao.blog.dto.BlogListDTO;
import com.hyxiao.blog.dto.BlogQueryDTO;
import com.hyxiao.response.BaseResponse;
import com.hyxiao.service.BlogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * @author Hyxiao
 * @date 12/08/2023 20:21
 * @description
 */
@RestController
@RequestMapping("/blog")
@Slf4j
public class BlogController {

    @Autowired
    private BlogService blogService;

    /**
     * 获取所有博客
     * 测试
     * @return 博客列表
     */
    @GetMapping("/list")
    public BaseResponse list(@RequestParam(required = false, defaultValue = "1") Integer page, @RequestParam(required = false, defaultValue = "10") Integer pageSize, @RequestParam(required = false) String category, @RequestParam(required = false) String keyword) {
        log.info("page: {}, pageSize: {}, category: {}, keyword: {}", page, pageSize, category, keyword);
        BlogQueryDTO blogQueryDTO = new BlogQueryDTO();
        blogQueryDTO.setPage(page);
        blogQueryDTO.setPageSize(pageSize);
        blogQueryDTO.setCategory(category);
        blogQueryDTO.setKeyword(keyword);

        BlogListDTO blogs = blogService.getBlogsByQuery(blogQueryDTO);
        return BaseResponse.success(blogs);
    }

    /**
     * 根据博客id获取博客
     *
     * @param id
     * @return 博客
     */
    @GetMapping("/{id}")
    public BaseResponse getBlogById(HttpServletRequest request, @PathVariable Long id) {
        log.info("id: {}", id);
        String host = getHost(request);
        BlogDTO blog = blogService.getBlogById(host, id);
        return BaseResponse.success(blog);
    }

    @GetMapping("/top")
    public BaseResponse getTopBlog() {
        BlogListDTO topBlogs = blogService.getTopBlogs();
        return BaseResponse.success(topBlogs);
    }

    @GetMapping("/category")
    public BaseResponse getCategory() {
        Map<String, Long> blogCategory = blogService.getBlogCategory();
        return BaseResponse.success(blogCategory);
    }

    /**
     * 创建博客
     *
     * @param blog
     * @return
     */
    @PostMapping("/create")
    public BaseResponse createBlog(@RequestBody BlogDTO blog) {
        blogService.createBlog(blog);
        return BaseResponse.success();
    }

    /**
     * 修改博客
     *
     * @param blog
     * @return
     */
    @PutMapping("/update")
    public BaseResponse updateBlog(@RequestBody BlogDTO blog) {
        blogService.updateBlog(blog);
        return BaseResponse.success();
    }

    /**
     * 删除博客
     *
     * @param id
     * @return
     */
    @DeleteMapping("/{id}")
    public BaseResponse deleteBlog(@PathVariable Long id) {
        blogService.deleteBlog(id);
        return BaseResponse.success();
    }

    /**
     * 增加博客浏览量
     *
     * @param id
     * @return
     */
    @PostMapping("/incrementview/{id}")
    public BaseResponse incrementView(@PathVariable Long id) {
        blogService.incrementView(id);
        return BaseResponse.success();
    }

    /**
     * 增加博客点赞量
     *
     * @param request
     * @param id
     * @return
     */
    @PostMapping("/incrementlike/{id}")
    public BaseResponse incrementLike(HttpServletRequest request, @PathVariable Long id) {
        String host = getHost(request);
        Boolean isLike = blogService.incrementLike(host, id);
        BlogDTO blogDTO = blogService.getBlogById(host, id);
        blogDTO.setIsLiked(isLike);
        return BaseResponse.success(blogDTO);
    }

    private String getHost(HttpServletRequest request) {
        return request.getRemoteHost();
    }

}
