package com.elias.interfaces;

import java.util.List;

import com.elias.aplicacion.ProductoServicio;
import com.elias.dominio.Producto;
import com.elias.interfaces.dto.ProductoDTO;
import com.elias.interfaces.dto.ProductoResponseDTO;
import com.elias.interfaces.mapper.ProductoMapper;

import jakarta.inject.Inject;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;


@Path("/productos")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class ProductoAPI {

    @Inject
    private ProductoServicio productoServicio;


    
    
    @POST
    public Response crear(ProductoDTO producto){
        Producto p = new Producto();
        p.setNombre(producto.getNombre());
        p.setPrecio(producto.getPrecio());
        p.setStock(producto.getStock());

        Producto p2= productoServicio.crear(p);
        ProductoResponseDTO prod= ProductoMapper.convertirToDTO(p2);
        
        return Response.status(Response.Status.CREATED).entity(prod).build();

    }

    @GET
    public Response obtenerTodos(){
        List<Producto> listaProducto= productoServicio.obtenerTodos();
        List<ProductoResponseDTO> listaProductosResponse= ProductoMapper.convertirToListaDTO(listaProducto);

        return Response.ok(listaProductosResponse).build();
    }

    @GET
    @Path("{id}")
    public Response obtenerPorId(@PathParam("id") Long id){

        try{
            Producto p= productoServicio.obtenerPorId(id);
            ProductoResponseDTO prd = ProductoMapper.convertirToDTO(p);
            return Response.ok(prd).build();
        }catch(IllegalArgumentException e){
            return Response.status(Response.Status.NOT_FOUND).entity(e.getMessage()).build();
        }
    }

    @PUT
    @Path("{id}")
    public Response actualizar(@PathParam("id")Long id, ProductoDTO dto){

        try{
            Producto p= new Producto(dto.getNombre(),dto.getPrecio(),dto.getStock());

            Producto productoActualizado= productoServicio.actualizar(id, p);
            ProductoResponseDTO prd = ProductoMapper.convertirToDTO(productoActualizado);

            return Response.ok(prd).build();

        }catch(IllegalArgumentException e){
            return Response.status(Response.Status.NOT_FOUND).entity(e.getMessage()).build();
            
        }

    }

    @DELETE
    @Path("{id}")
    public Response eliminar (@PathParam("id") Long id){
        try{
            productoServicio.eliminar(id);
            return Response.ok("Producto eliminado exitosamente").build();
        }catch(IllegalArgumentException e ){
            return Response.status(Response.Status.NOT_FOUND).entity(e.getMessage()).build();
        }
    }

    
}
