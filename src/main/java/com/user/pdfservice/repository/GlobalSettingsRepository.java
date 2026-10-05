package com.user.pdfservice.repository;

import com.user.pdfservice.model.entity.GlobalSettings;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GlobalSettingsRepository extends MongoRepository<GlobalSettings, String> {
}

