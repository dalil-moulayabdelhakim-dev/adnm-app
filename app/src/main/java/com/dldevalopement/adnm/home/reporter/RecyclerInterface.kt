package com.dldevalopement.adnm.home.reporter

/**
 * An interface to define a contract for handling click events on items within a RecyclerView.
 * This pattern helps in separating the concerns of the Adapter (which displays the data)
 * from the Activity or Fragment (which handles user interaction).
 */
interface RecyclerInterface {

    /**
     * Called when an item in the RecyclerView is clicked.
     * @param position The position of the clicked item in the data set.
     */
    fun onItemClick(position: Int)
}