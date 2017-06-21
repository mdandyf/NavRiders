package navriders.stuttgart.uni.com.example.mdand.navriders;

import java.util.List;

/**
 * Created by mdand on 6/19/2017.
 */

public interface Parser {
    List<Route> parse() throws RouteException;
}
