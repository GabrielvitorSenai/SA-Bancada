package com.tecdes.appsabancada.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "expedicao")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Expedicao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Integer numeroPedido;

    private Long pedidoId;

    private LocalDateTime dataSaida;

    @Column(unique = true)
    private Integer posicaoExpedicao;

    @ManyToOne(optional = true)
    @JoinColumn(name = "pedido_fk")
    private Pedido pedido;

    public Integer getOrderNumber() { return numeroPedido; }
    public void setOrderNumber(Integer orderNumber) { this.numeroPedido = orderNumber; }
}
