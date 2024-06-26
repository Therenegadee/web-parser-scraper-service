package parser.app.webscraper.services;

import io.micrometer.observation.annotation.Observed;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import parser.app.webscraper.DAO.interfaces.StorageDao;
import parser.app.webscraper.exceptions.BadRequestException;
import parser.app.webscraper.exceptions.NotFoundException;
import parser.app.webscraper.mappers.openapi.StorageMapper;
import parser.app.webscraper.models.Storage;
import parser.app.webscraper.services.interfaces.StorageService;
import parser.userService.openapi.model.StorageDTO;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class StorageServiceImpl implements StorageService {
    private final StorageDao storageDao;
    private final StorageMapper storageMapper;

    @Observed
    @Transactional
    @Override
    public ResponseEntity<Void> createStorage(Long userId) {
        Optional<Storage> storageOptional = storageDao.findByUserId(userId);
        if (storageOptional.isPresent()) {
            throw new BadRequestException(String.format("Storage for User With id %d is already exists!", userId));
        } else {
            Storage storage = new Storage();
            storage.setUserId(userId);
            storageDao.save(storage);
            return ResponseEntity
                    .status(201)
                    .build();
        }
    }

    @Override
    public Storage save(Storage storage) {
        return storageDao.save(storage);
    }

    @Observed
    @Transactional
    @Override
    public Storage findByStorageId(ObjectId storageId) {
        return storageDao
                .findById(storageId)
                .orElseThrow(() -> new NotFoundException(String.format("Storage with id %s wasn't found", storageId)));
    }

    @Observed
    @Transactional
    @Override
    public Storage findByUserId(Long userId) {
        return storageDao
                .findByUserId(userId)
                .orElseThrow(() -> new NotFoundException(String.format("Storage with user id %s wasn't found", userId)));
    }

    @Observed
    @Transactional
    @Override
    public ResponseEntity<Void> updateStorageById(String storageId, StorageDTO storageDTO) {
        Storage storage = storageMapper.toStorage(storageDTO);
        storageDTO.setId(storageId);
        storageDao.save(storage);
        return ResponseEntity
                .ok()
                .build();
    }

    @Observed
    @Transactional
    @Override
    public ResponseEntity<Void> updateStorageByUserId(Long userId, StorageDTO storageDTO) {
        Storage newStorage = storageMapper.toStorage(storageDTO);
        Storage storage = storageDao
                .findByUserId(userId)
                .orElseThrow(() -> new NotFoundException(String.format("Storage for user with id %d wasn't found", userId)));
        newStorage.setId(storage.getId());
        storageDao.save(newStorage);
        return ResponseEntity
                .ok()
                .build();
    }
}
