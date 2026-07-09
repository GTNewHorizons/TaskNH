package com.eldrinn.tasknh.cache;

import java.util.UUID;

import com.github.bsideup.jabel.Desugar;

@Desugar
public record PlayerEntry(UUID id, String name) {}
