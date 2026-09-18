package com.example.stentio.core.model;

import lombok.Getter;
import lombok.Setter;
import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Getter//lombok para nao precisa settar e dar get
@Setter//
@Table(name = "categorias_projeto")
public class CategoriaProjeto {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String nome;


}