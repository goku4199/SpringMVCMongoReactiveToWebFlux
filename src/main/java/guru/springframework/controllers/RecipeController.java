package guru.springframework.controllers;

import guru.springframework.commands.RecipeCommand;
import guru.springframework.exceptions.NotFoundException;
import guru.springframework.services.RecipeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.thymeleaf.exceptions.TemplateInputException;
import reactor.core.publisher.Mono;

import javax.validation.Valid;

@Controller
@Slf4j
public class RecipeController {

    public static final String RECIPE_RECIPEFORM = "recipe/recipeform";
    private final RecipeService recipeService;

    public RecipeController(RecipeService recipeService) {
        this.recipeService = recipeService;
    }

    @GetMapping("/recipe/{id}/show")
    public Mono<String> showById(@PathVariable String id, Model model){
        return recipeService.findById(id)
                .doOnNext(recipe -> model.addAttribute("recipe", recipe))//doOnNext() allows us to add some extra action that happens every time we get a new data item or Mono‘s doOnNext() allows us to attach a listener that will be triggered when the data is emitted.
                .map(rec -> "recipe/show")//converting from mono<Recipe> to Mono<String> and returning that
                .switchIfEmpty(Mono.error(new NotFoundException("recipe not found: " + id)));//Fallback to an alternative Mono if this mono is completed without data
    }

    @GetMapping("/recipe/new")
    public String newRecipe(Model model) {
        model.addAttribute("recipe", new RecipeCommand());
        return RECIPE_RECIPEFORM;
    }

    @GetMapping("/recipe/{id}/update")
    public String updateRecipe(@PathVariable String id, Model model){
        Mono<RecipeCommand> recipe = recipeService.findCommandById(id);
        model.addAttribute("recipe", recipe);//sending a mono recipe command to the view
        return RECIPE_RECIPEFORM;
    }

    @PostMapping("/recipe")
    public Mono<String> saveOrUpdate(@Valid @ModelAttribute("recipe") Mono<RecipeCommand> command) {//receving a mono recipe command from view
        return command
                .flatMap(recipeService::saveRecipeCommand)
                .map(recipe -> "redirect:/recipe/" + recipe.getId() + "/show")
                .doOnError(thr -> log.error("Error saving recipe"))
                .onErrorResume(WebExchangeBindException.class, thr -> Mono.just(RECIPE_RECIPEFORM));

        /*In Spring and reactive programming, flatMap is an operator used to transform the elements emitted by a reactive stream (such as a Mono or a Flux) while potentially triggering new asynchronous operations.
        It allows you to work with nested or multiple asynchronous operations in a structured and efficient way.
        Here's a brief explanation of how flatMap works.
        Transforming Elements: The flatMap operator takes each element emitted by the source reactive stream and applies a transformation function to it. This function returns another reactive stream (Mono or Flux) that represents the result of the transformation.
        Flattening Nested Streams: The key feature of flatMap is that it automatically flattens the nested reactive streams into a single reactive stream. This means that if your transformation function returns a Mono or a Flux, flatMap will ensure that the nested reactive stream's emissions are seamlessly integrated into the main stream.
        Concurrent Execution: flatMap also has the advantage of potentially executing the transformation functions concurrently, which can improve efficiency and overall performance.*/

        /* In Spring's reactive programming framework, specifically in the context of Project Reactor,
           the doOnError() operator is used to perform an action when an error occurs within a reactive stream.
           This operator allows you to define a callback that will be executed whenever an error is encountered while processing the elements of the stream. */

        /* In Spring's reactive programming framework, specifically in the context of Project Reactor,
           the onErrorResume() operator is used to handle errors that might occur during the processing of a reactive stream.
           This operator allows you to provide an alternative source of elements in case an error occurs,
           effectively "resuming" the stream with a fallback behavior */

    }

    @GetMapping("/recipe/{id}/delete")
    public Mono<String> deleteRecipe(@PathVariable String id){
        log.debug("Deleting recipe {}", id);
        return recipeService.deleteById(id)
                .thenReturn("redirect:/");
    }

    @ExceptionHandler({NotFoundException.class, TemplateInputException.class})
    @ResponseStatus(HttpStatus.NOT_FOUND)//for handling NotFoundException and TemplateInputException in RecipeController
    public String handleNotFound(Exception exc, Model model) {
        log.error("Handling not found exception {}", exc.getMessage());
        model.addAttribute("exception", exc);
        return "error404";
    }

}
