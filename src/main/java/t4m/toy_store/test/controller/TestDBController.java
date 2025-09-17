package t4m.toy_store.test.controller;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.bind.annotation.*;
import t4m.toy_store.test.model.TestEntity;
import t4m.toy_store.test.repository.TestRepository;

import java.util.List;

@Data
@RestController
@RequestMapping("/api/db")
public class TestDBController {
    private TestRepository repository;

    public TestDBController(TestRepository repository) {
        this.repository = repository;
    }

    @PostMapping("/add")
    public TestEntity add(@RequestParam String msg) {
        TestEntity entity = new TestEntity();
        entity.setMessage(msg);
        return repository.save(entity);
    }

    @GetMapping("/all")
    public List<TestEntity> all() {
        return repository.findAll();
    }
}
