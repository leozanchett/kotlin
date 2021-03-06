package com.example.android.pets.data;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

public class PetProvider extends ContentProvider {

    /**
     * Tag for the log messages
     */
    public static final String LOG_TAG = PetProvider.class.getSimpleName();

    private PetsDBHelper mDbHelper;

    private static final int PETS = 100;
    private static final int PET_ID = 101;
    public static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        sUriMatcher.addURI(PetsContract.PetsUri.CONTENT_AUTHORITY, PetsContract.PetsUri.PATH_PETS, PETS);
        sUriMatcher.addURI(PetsContract.PetsUri.CONTENT_AUTHORITY, PetsContract.PetsUri.PATH_PETS + "/#", PET_ID);
    }

    @Override
    public boolean onCreate() {
        mDbHelper = new PetsDBHelper(getContext());
        return true;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        SQLiteDatabase database = mDbHelper.getReadableDatabase();
        Cursor cursor;
        int match = sUriMatcher.match(uri);
        switch (match) {
            case PETS:
                cursor = database.query(PetsContract.PetsEntry.TABLE_NAME, projection, null, null, null, null, sortOrder);
                break;
            case PET_ID:
                selection = PetsContract.PetsEntry._ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
                cursor = database.query(PetsContract.PetsEntry.TABLE_NAME, projection, selection, selectionArgs, null, null, sortOrder);
                break;
            default:
                throw new IllegalArgumentException("Cannot query unknown URI " + uri);
        }
        cursor.setNotificationUri(getContext().getContentResolver(), uri);
        return cursor;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case PETS:
                return PetsContract.PetsUri.CONTENT_LIST_TYPE;
            case PET_ID:
                return PetsContract.PetsUri.CONTENT_ITEM_TYPE;
            default:
                throw new IllegalStateException("Unknown URI " + uri + " with match " + match);
        }
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues contentValues) {
        final int match = sUriMatcher.match(uri);
        if (match == PETS) {
            getContext().getContentResolver().notifyChange(uri, null);
            return InsertPet(contentValues, uri);
        } else {
            throw new IllegalArgumentException("Cannot query unknown URI " + uri);
        }

    }

    private Uri InsertPet(ContentValues contentValues, Uri uri) {
        VerifyValues(contentValues);
        SQLiteDatabase database = mDbHelper.getWritableDatabase();
        long newId = database.insert(PetsContract.PetsEntry.TABLE_NAME, null, contentValues);
        return ContentUris.withAppendedId(uri, newId);
    }


    private void VerifyValues(@NonNull ContentValues contentValues) {
        final String name = contentValues.getAsString(PetsContract.PetsEntry.COLUMN_PET_NAME);
        if ((name == null) || (name.isEmpty())) {
            throw new IllegalArgumentException("Pet requires a name");
        }
        final int weigth = contentValues.getAsInteger(PetsContract.PetsEntry.COLUMN_PET_WEIGTH);
        if (weigth <= 0) {
            throw new IllegalArgumentException("Invalid weigth");
        }
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        final int match = sUriMatcher.match(uri);
        int idDelete;
        SQLiteDatabase database = mDbHelper.getWritableDatabase();
        switch (match) {
            case PET_ID:
                selection = PetsContract.PetsEntry._ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
                idDelete = database.delete(PetsContract.PetsEntry.TABLE_NAME, selection, selectionArgs);
                break;
            case PETS:
                idDelete = database.delete(PetsContract.PetsEntry.TABLE_NAME, selection, selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Cannot query unknown URI " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return idDelete;
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues contentValues, @Nullable String selection, @Nullable String[] selectionArgs) {
        final int match = sUriMatcher.match(uri);
        int idUpdated;
        if (match == PET_ID) {
            VerifyValues(contentValues);
            SQLiteDatabase database = mDbHelper.getWritableDatabase();
            selection = PetsContract.PetsEntry._ID + "=?";
            selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
            idUpdated = database.update(PetsContract.PetsEntry.TABLE_NAME, contentValues, selection, selectionArgs);
        } else {
            throw new IllegalArgumentException("Cannot query unknown URI " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return idUpdated;
    }
}
