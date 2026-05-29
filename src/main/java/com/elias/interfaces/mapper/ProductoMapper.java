package com.elias.interfaces.mapper;

import java.util.ArrayList;
import java.util.List;

import com.elias.dominio.Producto;
import com.elias.interfaces.dto.ProductoResponseDTO;

public class ProductoMapper {
    
    public static ProductoResponseDTO convertirToDTO(Producto producto){
        ProductoResponseDTO prd = new ProductoResponseDTO(producto.getId(), producto.getNombre(),producto.getPrecio(), producto.getStock());

        return prd;
    }

    public static List<ProductoResponseDTO> convertirToListaDTO(List<Producto> productos){
        List <ProductoResponseDTO> listaPRD = new ArrayList<>();
        for(Producto p : productos){
            listaPRD.add(convertirToDTO(p));
        }

        if(listaPRD.isEmpty()){
            throw new IllegalArgumentException("La lista no contiene informacion");
        }else{
            return listaPRD;
        }
            

    }
}
