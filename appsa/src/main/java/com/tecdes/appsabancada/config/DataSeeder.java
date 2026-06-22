package com.tecdes.appsabancada.config;

import com.tecdes.appsabancada.entity.Estoque;
import com.tecdes.appsabancada.entity.PosicaoEstoque;
import com.tecdes.appsabancada.repository.EstoqueRepository;
import com.tecdes.appsabancada.repository.PosicaoEstoqueRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Popula as 28 posições físicas do estoque e alguns itens de demonstração,
 * apenas quando o banco está vazio. Útil para a apresentação na banca, para
 * que o Mapa Visual já abra com posições coloridas.
 *
 * Ative/desative no application.properties:
 *   app.seed.estoque=true   (padrão: true)
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.seed.estoque", havingValue = "true", matchIfMissing = true)
public class DataSeeder implements CommandLineRunner {

    private static final int TOTAL_POSICOES = 28;

    private final PosicaoEstoqueRepository posicaoRepository;
    private final EstoqueRepository estoqueRepository;

    @Override
    public void run(String... args) {

        if (posicaoRepository.count() > 0) {
            return; // já inicializado, não duplica
        }

        // Cria as 28 posições físicas (todas disponíveis inicialmente).
        for (int i = 1; i <= TOTAL_POSICOES; i++) {
            posicaoRepository.save(PosicaoEstoque.builder()
                    .posicao(i)
                    .disponivel(true)
                    .build());
        }

        // Itens de demonstração: cor 1=preto, 2=vermelho, 3=azul.
        // Posição -> cor (deixa o restante vazio = cinza no mapa).
        int[][] demo = {
                {1, 1}, {2, 1}, {3, 2}, {5, 3}, {6, 3},
                {8, 2}, {9, 2}, {12, 1}, {15, 3}, {20, 2}, {21, 1}, {25, 3}
        };

        for (int[] par : demo) {
            posicaoRepository.findByPosicao(par[0]).ifPresent(pos -> {
                pos.setDisponivel(false);
                posicaoRepository.save(pos);
                estoqueRepository.save(Estoque.builder()
                        .cor(par[1])
                        .quantidade(10)
                        .posicaoEstoque(pos)
                        .build());
            });
        }

        System.out.println("=============================");
        System.out.println("DataSeeder: 28 posições + " + demo.length + " itens de estoque criados.");
        System.out.println("=============================");
    }
}
