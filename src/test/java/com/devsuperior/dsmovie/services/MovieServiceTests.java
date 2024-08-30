package com.devsuperior.dsmovie.services;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.devsuperior.dsmovie.dto.MovieDTO;
import com.devsuperior.dsmovie.entities.MovieEntity;
import com.devsuperior.dsmovie.repositories.MovieRepository;
import com.devsuperior.dsmovie.services.exceptions.DatabaseException;
import com.devsuperior.dsmovie.services.exceptions.ResourceNotFoundException;
import com.devsuperior.dsmovie.tests.MovieFactory;

import jakarta.persistence.EntityNotFoundException;

@ExtendWith(SpringExtension.class)
public class MovieServiceTests {
	
	@InjectMocks
	private MovieService service;

	@Mock
	private MovieRepository repository;

	private String title;
	private MovieEntity movie;
	private MovieDTO movieDTO;
	private PageImpl<MovieEntity> page;
	private Long existingMovieId, nonExistingMovieId, dependentMovieId;
	
	
	@BeforeEach
	void setUp() throws Exception {
		title = "Witcher";
		movie = MovieFactory.createMovieEntity();
		movieDTO = new MovieDTO(movie);
		page = new PageImpl<>(List.of(movie));
		existingMovieId = 1L;
		nonExistingMovieId = 2L;
		dependentMovieId = 3L;


		Mockito.when(repository.searchByTitle(any(), (Pageable) any())).thenReturn(page);

		Mockito.when(repository.findById(existingMovieId)).thenReturn(Optional.of(movie));
		Mockito.when(repository.findById(nonExistingMovieId)).thenReturn(Optional.empty());

		Mockito.when(repository.save(any())).thenReturn(movie);

		Mockito.when(repository.getReferenceById(existingMovieId)).thenReturn(movie);
		Mockito.when(repository.getReferenceById(nonExistingMovieId)).thenThrow(EntityNotFoundException.class);
		
		Mockito.when(repository.existsById(existingMovieId)).thenReturn(true);
		Mockito.when(repository.existsById(dependentMovieId)).thenReturn(true);
		Mockito.when(repository.existsById(nonExistingMovieId)).thenReturn(false);

		Mockito.doNothing().when(repository).deleteById(existingMovieId);
		doThrow(DataIntegrityViolationException.class).when(repository).deleteById(dependentMovieId);
	}
	
	@Test
	public void findAllShouldReturnPagedMovieDTO() {
		Pageable pageable = PageRequest.of(0, 10); 
		Page<MovieDTO> moviesDTO = service.findAll(title, pageable);

		Assertions.assertNotNull(moviesDTO);
		Assertions.assertEquals(moviesDTO.getSize(), 1);
		Assertions.assertEquals(moviesDTO.iterator().next().getTitle(), movie.getTitle());
		
	}
	
	@Test
	public void findByIdShouldReturnMovieDTOWhenIdExists() {

		MovieDTO dto = service.findById(existingMovieId);
		Assertions.assertNotNull(dto);
		Assertions.assertEquals(dto.getId(), existingMovieId);
	}
	
	@Test
	public void findByIdShouldThrowResourceNotFoundExceptionWhenIdDoesNotExist() {

		Assertions.assertThrows(ResourceNotFoundException.class, () -> {
			service.findById(nonExistingMovieId);
		});
	}
	
	@Test
	public void insertShouldReturnMovieDTO() {

		MovieDTO dto = service.insert(movieDTO);

		Assertions.assertNotNull(dto);
		Assertions.assertEquals(dto.getId(), existingMovieId);
		Assertions.assertEquals(dto.getTitle(), "Test Movie");
		
	}
	
	@Test
	public void updateShouldReturnMovieDTOWhenIdExists() {

		movie.setTitle("New Movie");
		movieDTO = new MovieDTO(movie);
		MovieDTO dto = service.update(existingMovieId, movieDTO);

		Assertions.assertNotNull(dto);
		Assertions.assertEquals(dto.getTitle(), "New Movie");
		
	}
	
	@Test
	public void updateShouldThrowResourceNotFoundExceptionWhenIdDoesNotExist() {

		Assertions.assertThrows(ResourceNotFoundException.class, () -> {
			service.update(nonExistingMovieId, movieDTO);
		});
	}
	
	@Test
	public void deleteShouldDoNothingWhenIdExists() {

		Assertions.assertDoesNotThrow(()->{
			service.delete(dependentMovieId);
		});
	}
	
	@Test
	public void deleteShouldThrowResourceNotFoundExceptionWhenIdDoesNotExist() {

		Assertions.assertThrows(ResourceNotFoundException.class, () -> {
			service.delete(nonExistingMovieId);
		});
	}
	
	@Test
	public void deleteShouldThrowDatabaseExceptionWhenDependentId() {
	
		Assertions.assertThrows(DatabaseException.class, () -> {
			service.delete(dependentMovieId);
		});
	}
}
