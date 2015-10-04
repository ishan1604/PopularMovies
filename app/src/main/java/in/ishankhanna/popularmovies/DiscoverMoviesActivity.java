package in.ishankhanna.popularmovies;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;

import java.util.Collections;
import java.util.List;

import in.ishankhanna.popularmovies.adapters.MovieTilesAdapter;
import in.ishankhanna.popularmovies.db.MovieDAO;
import in.ishankhanna.popularmovies.models.Movie;
import in.ishankhanna.popularmovies.models.MovieResponse;
import in.ishankhanna.popularmovies.utils.API;
import in.ishankhanna.popularmovies.utils.comparators.MoviePopularityComparator;
import in.ishankhanna.popularmovies.utils.comparators.MovieRatingComparator;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class DiscoverMoviesActivity extends AppCompatActivity {
    private static final String KEY_LAST_SORT_BY = "last_sort_by";
    private static final String KEY_LAST_GRID_POSITION = "last_grid_position";
    private final String TAG = "DiscoverMoviesActivity";
    private final static int SORT_BY_RATING = 1;
    private final static int SORT_BY_POPULARITY = 2;
    private final static int SORT_BY_FAVORITES = 3;

    private final static String SORT_STRING_POPULARITY = "popularity.desc";
    private final static String SORT_STRING_VOTE_AVERAGE = "vote_average.desc";

    GridView moviesGridView;
    MovieTilesAdapter movieTilesAdapter;
    List<Movie> movies;
    private int currentlySortingBy = -1;
    private int currentGridPosition = -1;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_discover_movies);

        moviesGridView = (GridView) findViewById(R.id.gridViewMovies);
        if (savedInstanceState != null) {
            currentlySortingBy = savedInstanceState.getInt(KEY_LAST_SORT_BY, SORT_BY_POPULARITY);
            currentGridPosition = savedInstanceState.getInt(KEY_LAST_GRID_POSITION, 0);
        } else {
            currentlySortingBy = SORT_BY_POPULARITY;
        }

        sortMoviesGrid(currentlySortingBy);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_discover_movies, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch (id) {
            case R.id.action_sort_by_popularity : sortMoviesGrid(SORT_BY_POPULARITY);
                break;
            case R.id.action_sort_by_rating: sortMoviesGrid(SORT_BY_RATING);
                break;
            case R.id.action_show_favorites: sortMoviesGrid(SORT_BY_FAVORITES);
        }

        return super.onOptionsItemSelected(item);
    }

    private void sortMoviesByRating() {
        Collections.sort(movies, new MovieRatingComparator());
    }
    private void sortMoviesByPopularity() {
        Collections.sort(movies, new MoviePopularityComparator());
    }

    private void sortMoviesGrid(int sortBy) {
        currentlySortingBy = sortBy;
        switch(sortBy) {
            case SORT_BY_POPULARITY: inflateMoviesGridByDataFromNetwork(SORT_STRING_POPULARITY);
                break;
            case SORT_BY_RATING: inflateMoviesGridByDataFromNetwork(SORT_STRING_VOTE_AVERAGE);
                break;
            case SORT_BY_FAVORITES: inflateMoviesGridByDataFromDatabase();
        }

    }

    private void inflateMoviesGridByDataFromNetwork(String sortBy) {

        // pixels, dpi
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        int heightPixels = metrics.heightPixels;
        final int widthPixels = metrics.widthPixels;

        API.mMoviesService.getLatestMovies(sortBy, new Callback<MovieResponse>() {
            @Override
            public void success(MovieResponse movieDbResponse, Response response) {

                Log.d(TAG, "Movies Fetched : " + movieDbResponse.getMovies().size());
                movies = movieDbResponse.getMovies();
                movieTilesAdapter = new MovieTilesAdapter(getApplicationContext(), movies, widthPixels);
                moviesGridView.setAdapter(movieTilesAdapter);
                setMovieGridItemClickListener();
            }

            @Override
            public void failure(RetrofitError error) {

            }
        });
    }

    private void inflateMoviesGridByDataFromDatabase() {

        // pixels, dpi
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        int heightPixels = metrics.heightPixels;
        final int widthPixels = metrics.widthPixels;
        MovieDAO movieDAO = new MovieDAO(this);
        movieDAO.openForReadOnly();
        movies = movieDAO.getAllMovies();
        movieDAO.close();
        Log.d(TAG, "" + movies.size());
        movieTilesAdapter = new MovieTilesAdapter(this, movies, widthPixels);
        moviesGridView.setAdapter(movieTilesAdapter);
        setMovieGridItemClickListener();

    }

    private void setMovieGridItemClickListener() {
        moviesGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Log.d(TAG, movies.get(position).toString());

                Intent detailsActivityIntent = new Intent(DiscoverMoviesActivity.this, MovieDetailsActivity.class);
                detailsActivityIntent.putExtra("movie", movies.get(position));
                startActivity(detailsActivityIntent);

            }
        });
        if (currentGridPosition >= 0)
            moviesGridView.smoothScrollToPosition(currentGridPosition);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_LAST_SORT_BY, currentlySortingBy);
        outState.putInt(KEY_LAST_GRID_POSITION, moviesGridView.getFirstVisiblePosition());
    }

}
