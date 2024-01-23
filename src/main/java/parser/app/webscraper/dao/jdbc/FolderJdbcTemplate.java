package parser.app.webscraper.dao.jdbc;

import io.micrometer.observation.annotation.Observed;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import parser.app.webscraper.dao.jdbc.interfaces.FolderDao;
import parser.app.webscraper.dao.jdbc.interfaces.UserParserSettingsDao;
import parser.app.webscraper.exceptions.NotFoundException;
import parser.app.webscraper.mappers.jdbc.FolderMapper;
import parser.app.webscraper.models.Folder;
import parser.app.webscraper.models.UserParserSetting;
import parser.userService.openapi.model.FolderOpenApi;

import java.sql.Array;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.*;

import static java.sql.Types.NULL;

@Observed
@Repository
@RequiredArgsConstructor
public class FolderJdbcTemplate implements FolderDao {
    private final JdbcTemplate jdbcTemplate;
    private final FolderMapper folderMapper;
    private final UserParserSettingsDao userParserSettingsDao;

    @Transactional
    @Override
    public Optional<Folder> findByFolderId(Long id) {
        String query = """
                WITH RECURSIVE folders AS (
                  SELECT * FROM folder f
                  WHERE f.id = ?
                 \s
                  UNION ALL
                 \s
                  SELECT subf.* FROM folder subf
                  JOIN folders fs ON subf.parent_folder_id = fs.id
                 )
                 SELECT * FROM folders fs
                 ORDER BY fs.parent_folder_id NULLS FIRST, fs.id;
                """;
        return jdbcTemplate
                .query(query, folderMapper, id)
                .stream()
                .findFirst();
    }

    @Transactional
    @Override
    public List<Folder> findByParentFolderId(Long id) {
        String query = """
                WITH RECURSIVE folders AS (
                  SELECT * FROM folder f
                  WHERE f.parent_folder_id = ?
                 \s
                  UNION ALL
                 \s
                  SELECT subf.* FROM folder subf
                  JOIN folders fs ON subf.parent_folder_id = fs.id
                 )
                 SELECT * FROM folders fs
                 ORDER BY fs.parent_folder_id NULLS FIRST, fs.id;
                """;
        return jdbcTemplate
                .query(query, folderMapper, id);
    }

//    @Transactional
//    @Override
//    public List<Folder> findByFolderId(long minId, long maxId) {
//        String query = """
//                WITH RECURSIVE folders AS (
//                  SELECT * FROM folder f
//                  WHERE f.parent_folder_id between ? and ?
//                 \s
//                  UNION ALL
//                 \s
//                  SELECT subf.* FROM folder subf
//                  JOIN folders fs ON subf.parent_folder_id = fs.id
//                 )
//                 SELECT * FROM folders fs
//                 ORDER BY fs.parent_folder_id NULLS FIRST, fs.id;
//                """;
//        return jdbcTemplate.query(query, folderMapper, minId, maxId);
//    }

    @Transactional
    @Override
    public Folder save(FolderOpenApi folder) {
        if (Objects.isNull(folder)) throw new IllegalArgumentException("Folder is Null!");
        String query = "INSERT INTO folder (user_id,parent_folder_id) " +
                "VALUES(?,?) RETURNING id";

        KeyHolder keyHolder = new GeneratedKeyHolder();

        Long parentId = (folder.getParentFolderId() != null) ? folder.getParentFolderId() : null;

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
            int index = 1;
            ps.setString(index++, folder.getName());
            Array tags = connection.createArrayOf("TEXT", folder.getTags().toArray());
            ps.setArray(index++, tags);
            ps.setLong(index++, folder.getStorageId());
            if (parentId == null) {
                ps.setNull(index++, NULL);
            } else {
                ps.setLong(index++, parentId);
            }
            return ps;
        }, keyHolder);

        return findByFolderId(Objects.requireNonNull(keyHolder.getKey()).longValue())
                .orElseThrow(() -> new RuntimeException("Folder doesn't exist"));
    }

    @Transactional
    @Override
    public Folder update(FolderOpenApi folder) {
        if (Objects.isNull(folder)) throw new IllegalArgumentException("Folder is Null!");
        Long id = folder.getId();
        return updateById(id, folder);
    }

    @Transactional
    @Override
    public Folder updateById(Long id, FolderOpenApi folder) {
        if (Objects.isNull(folder)) throw new IllegalArgumentException("Folder is Null!");
        if (Objects.nonNull(id) && findByFolderId(id).isPresent()) {
            String query = "UPDATE folder SET name=?, tags=?, storage_id=?, " +
                    "parent_folder_id=? WHERE id=?";

            Long parentId = (folder.getParentFolderId() != null) ? folder.getParentFolderId() : null;

            int rows = jdbcTemplate.update(
                    query,
                    folder.getName(), new ArrayList<>(folder.getTags()), folder.getStorageId(), parentId,
                    id
            );

            if (rows != 1) {
                throw new RuntimeException("Invalid request in SQL: " + query);
            }
            return findByFolderId(id).get();
        } else {
            throw new NotFoundException(String.format("Folder with id %d wasn't found", id));
        }
    }

    @Transactional
    @Override
    public int deleteById(Long id) {
        Optional<Folder> folderOpt = findByFolderId(id);
        if (Objects.nonNull(id) && folderOpt.isPresent()) {
            String query = "DELETE FROM folder WHERE id=?";
            Folder folder = folderOpt.get();

            //todo: batch update delete
            List<UserParserSetting> userParserSettingsToUpdate = userParserSettingsDao.findAllByParentFolderId(id);
            userParserSettingsToUpdate
                    .forEach(userParserSetting -> userParserSetting.setParentFolder(
                            Objects.isNull(folder.getParentFolder()) ? null : folder.getParentFolder())
                    );
            userParserSettingsDao.update(userParserSettingsToUpdate);
            return jdbcTemplate.update(query, id);
        } else {
            throw new NotFoundException(String.format("Folder with id %d wasn't found", id));
        }
    }

    @Transactional
    @Override
    public int delete(Folder folder) {
        if (Objects.isNull(folder)) throw new IllegalArgumentException("Folder is Null!");
        Long id = folder.getId();
        return deleteById(id);
    }
}