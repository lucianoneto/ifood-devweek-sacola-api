package me.dio.sacola.service.impl;

import lombok.RequiredArgsConstructor;
import me.dio.sacola.enumeration.FormaPagamento;
import me.dio.sacola.model.Item;
import me.dio.sacola.model.Restaurante;
import me.dio.sacola.model.Sacola;
import me.dio.sacola.repository.ItemRepository;
import me.dio.sacola.repository.ProdutoRepository;
import me.dio.sacola.repository.SacolaRepository;
import me.dio.sacola.resource.dto.ItemDto;
import me.dio.sacola.service.SacolaService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Service
public class SacolaServiceImpl implements SacolaService {

    private final SacolaRepository sacolaRepository;
    private final ProdutoRepository produtoRepository;
    private final ItemRepository itemRepository;

    @Override
    public Item incluirItemNaSacola(ItemDto itemDto) {

        Sacola sacola = verSacola(itemDto.getSacolaId());

        if (sacola.isFechada())
            throw new RuntimeException("Esta sacola está fechada!");

        Item novoItem = Item.builder()
                .quantidade(itemDto.getQuantidade())
                .sacola(sacola)
                .produto(produtoRepository.findById(itemDto.getProdutoId()).orElseThrow(
                        () -> {
                            throw new RuntimeException("Esse produto não existe!");
                        }))
                .build();

        List<Item> itensSacola = sacola.getItens();

        if (itensSacola.isEmpty())
            itensSacola.add(novoItem);

        else {
            Restaurante restaurante = itensSacola.get(0).getProduto().getRestaurante();
            Restaurante restauranteNovoItem = novoItem.getProduto().getRestaurante();

            if (restaurante.equals(restauranteNovoItem))
                itensSacola.add(novoItem);
            else
                throw new RuntimeException("Não é possível adicionar produtos de restaurantes diferentes. Feche a sacola ou esvazie.");
        }

        List<Double> valorItens = new ArrayList<>();

        for (Item itemDaSacola : itensSacola) {
            double valorTotalItem = itemDaSacola.getProduto().getValorUnitario() * itemDaSacola.getQuantidade();
            valorItens.add(valorTotalItem);
        }

        double valorTotalSacola = valorItens.stream()
                .mapToDouble(valorCadaItem -> valorCadaItem)
                .sum();

        sacola.setValorTotal(valorTotalSacola);
        sacolaRepository.save(sacola);

        return novoItem;
    }

    @Override
    public Sacola verSacola(Long id) {
        return sacolaRepository.findById(id).orElseThrow(
                () -> {
                    throw new RuntimeException("Essa sacola não existe!");
                }
        );
    }

    @Override
    public Sacola fecharSacola(Long id, int numeroFormaPagamento) {

        Sacola sacola = verSacola(id);

        if (sacola.getItens().isEmpty())
            throw new RuntimeException("Inclua itens na sacola!");

        FormaPagamento formaPagamento = numeroFormaPagamento == 0 ? FormaPagamento.DINHEIRO : FormaPagamento.MAQUINETA;

        sacola.setFormaPagamento(formaPagamento);
        sacola.setFechada(true);

        return sacolaRepository.save(sacola);
    }
}