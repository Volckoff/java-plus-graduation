package ru.practicum.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.model.Category;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END" +
            " FROM Category c WHERE LOWER(TRIM(c.name)) = LOWER(TRIM(:name))")
    boolean existsByNameIgnoreCaseAndTrim(@Param("name") String name);

}
