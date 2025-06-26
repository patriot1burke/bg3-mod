package org.bg3.forge;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import java.util.List;

@Entity
public class Item extends PanacheEntity {
    public String title;

    @Column(length = 4000)
    public String description;


    public static List<Item> searchByTitleLike(String title) {
      return find("title like ?1", "%" + title + "%").list();
    }

    @Override
    public String toString() {
        return title + " - " + description;
    }
}

