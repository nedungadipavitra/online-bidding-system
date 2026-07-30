package com.onlinebidding.product_service.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.onlinebidding.product_service.dto.CategoryDto;
import com.onlinebidding.product_service.exception.GlobalExceptionHandler;
import com.onlinebidding.product_service.service.CategoryService;

@ExtendWith(MockitoExtension.class)
class CategoryControllerTest {

    private MockMvc mockMvc;

    @Mock
    private CategoryService categoryService;

    @InjectMocks
    private CategoryController categoryController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(categoryController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void addCategory_withAdminRole_returns201Created() throws Exception {
        CategoryDto responseDto = CategoryDto.builder()
                .id(1L)
                .name("Electronics")
                .description("Electronic gadgets")
                .build();

        when(categoryService.addCategory(any(CategoryDto.class))).thenReturn(responseDto);

        String jsonPayload = """
                {
                    "name": "Electronics",
                    "description": "Electronic gadgets"
                }
                """;

        mockMvc.perform(post("/categories")
                .header("X-User-Role", "ADMIN")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Electronics"));
    }

    @Test
    void addCategory_withUserRole_returns403Forbidden() throws Exception {
        String jsonPayload = """
                {
                    "name": "Electronics",
                    "description": "Electronic gadgets"
                }
                """;

        mockMvc.perform(post("/categories")
                .header("X-User-Role", "USER")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonPayload))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Access denied. Only ADMIN can create categories."));
    }

    @Test
    void addCategory_withoutRoleHeader_returns403Forbidden() throws Exception {
        String jsonPayload = """
                {
                    "name": "Electronics",
                    "description": "Electronic gadgets"
                }
                """;

        mockMvc.perform(post("/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonPayload))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Access denied. Only ADMIN can create categories."));
    }
}
